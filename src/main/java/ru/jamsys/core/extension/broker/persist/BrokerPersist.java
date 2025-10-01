package ru.jamsys.core.extension.broker.persist;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.AsyncFileWriterRolling;
import ru.jamsys.core.extension.async.writer.DataReadWrite;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// SSD 80K IOPS при 4KB на блок = 320 MB/s на диске в режиме RandomAccess и до 550 MB/s в линейной записи (секвентальная)
// Для гарантии записи надо использовать StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DSYNC
// (в таком режиме atime, mtime, размер файла может быть не синхранизован, но данные при восстановлении будут вычитаны
// корректно)
// SIZE - прикольно бы было знать общий объём, но что бы его вычислить надо прочитать все файлы, а если не перечитывать
// то commit X будет делать decrementAndGet и возможна ситуация выйти в минус, остановился на том, что метрики size
// лучше не делать

@Getter
public class BrokerPersist<T extends ByteSerializable> extends AbstractManagerElement implements PropertyListener {

    private final String ns;

    private final String key;

    private final BrokerPersistRepositoryProperty property = new BrokerPersistRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicInteger tpsEnqueue = new AtomicInteger(0);

    private ManagerConfiguration<AsyncFileWriterRolling<BlockData<T>>> writerManagerConfiguration;

    // Конфиг может быть удалён, только если файл полностью обработан, до этого момента он должен быть тут 100%
    private final Map<String, ManagerConfiguration<Rider>> mapRiderConfiguration = new ConcurrentHashMap<>();

    // Для того, что бы наполнять queue надо брать существующие CommitController по порядку и доить их
    private final ConcurrentLinkedDeque<ManagerConfiguration<Rider>> queueRiderConfiguration = new ConcurrentLinkedDeque<>();

    private Function<byte[], T> restoreElementFromByte;

    public BrokerPersist(String ns, String key) {
        this.ns = ns;
        this.key = key;
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                property,
                getCascadeKey(ns)
        );
        dataWriterInit();
    }

    public void setup(Function<byte[], T> restoreElementFromByte) {
        this.restoreElementFromByte = restoreElementFromByte;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                .append("propertyDispatcherNs", propertyDispatcher.getNs())
                //.append("brokerPersistRepositoryProperty", property)
                ;
    }

    public void dataWriterInit() {
        String key = "X";
        if (writerManagerConfiguration != null) {
            App.get(Manager.class).remove(AsyncFileWriterRolling.class, key, getCascadeKey(ns));
        }
        writerManagerConfiguration = ManagerConfiguration.getInstance(
                getCascadeKey(ns),
                key,
                AsyncFileWriterRolling.class,
                asyncFileWriterRolling -> {
                    asyncFileWriterRolling.setupRepositoryProperty(property);
                    asyncFileWriterRolling.setupOnFileSwap(this::onDataFileSwap);
                    asyncFileWriterRolling.setupOnWrite(this::onDataWrite);
                    // Если по каким-то причинам закрывается файл данных, надо оповестить rider
                    // При обычной работе происходит просто onSwap и едем дальше, а при завершении работы приложения
                    // или просто BrokerPersist отъехал от дел, буду закрываться ресурсы, вот тут тоже вызовется
                    asyncFileWriterRolling.getListOnPostShutdown().add(() -> {
                        ManagerConfiguration<Rider> riderConfiguration = mapRiderConfiguration
                                .get(asyncFileWriterRolling.getFilePath());
                        if (riderConfiguration != null && riderConfiguration.isAlive()) {
                            riderConfiguration.get().getQueueRetry().setFinishState(true);
                        }
                    });
                }
        );

        // При старте мы должны поднять все Rider в карту mapRiderConfiguration это надо, что бы наполнять
        // очередь, так как она цикличная
        UtilFile
                .getFilesRecursive(property.getDirectory())
                .stream()
                .filter(s -> s.endsWith(".control"))
                .sorted()
                .toList()
                .forEach(filePathControl -> {
                    String relativePathData = UtilFile.getRelativePath(
                            property.getDirectory(),
                            filePathControlToData(filePathControl)
                    );
                    if (UtilFile.ifExist(relativePathData)) {
                        getRiderConfiguration(relativePathData, true);
                    } else {
                        App.error(new RuntimeException(
                                "File does not exist: " + relativePathData
                                        + "; Remove: " + filePathControl
                        ));
                        try {
                            UtilFile.remove(filePathControl);
                        } catch (IOException e) {
                            App.error(new ForwardException(e));
                        }
                    }
                });
        removeCycleTransactionFile();
    }

    // Вызывается из планировщика выполняющего запись в файл (однопоточное использование)
    public void onDataWrite(String filePath, List<BlockData<T>> listBlockData) {
        ManagerConfiguration<Rider> riderConfiguration = mapRiderConfiguration.get(filePath);
        if (riderConfiguration == null) {
            throw new RuntimeException("Rider(" + filePath + ") not found");
        }
        // Бывает такое, что конфигурации останавливаются, из-за того, что не используются. Используются, это когда
        // в них коммитят позиции, извлекают из них не закоммиченные позиции
        riderConfiguration.executeIfAlive(rider -> {
            for (BlockData<T> blockData : listBlockData) {
                blockData.setRiderConfiguration(riderConfiguration);
                rider.onWriteData(blockData);
            }
        });
    }

    // Вызывается из планировщика выполняющего запись в файл (однопоточное использование)
    private void removeRiderIfComplete(Rider rider) {
        if (rider.getQueueRetry().isProcessed()) {
            String filePathData = filePathControlToData(rider.getFilePathControl());
            ManagerConfiguration<Rider> removedRiderConfiguration = mapRiderConfiguration.remove(filePathData);
            // Если контроллер найден по имени файла, удалим и из очереди
            if (removedRiderConfiguration != null) {
                queueRiderConfiguration.remove(removedRiderConfiguration);
                App.get(Manager.class).remove(
                        removedRiderConfiguration.getCls(),
                        removedRiderConfiguration.getKey(),
                        removedRiderConfiguration.getNs()
                );
            }
            removeCycleTransactionFile();
        }
    }

    private void removeCycleTransactionFile() {
        // Если суммарно есть 100 файлов, 10 из которых в работе (то есть по ним есть .control файлы) и
        // property.getCount() = 3, после выполнения данной функции останется 13 файлов .afwr и 10 .commit файлов,
        // так как они тут не удаляются
        List<String> filesToRemove = UtilFile.getFilesRecursive(property.getDirectory())
                .stream()
                .filter(path -> path.endsWith(".afwr"))
                .map(path -> Map.entry(path, UtilFile.getRelativePath(property.getDirectory(), path)))
                .filter(entry -> !mapRiderConfiguration.containsKey(entry.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        filesToRemove.stream()
                .limit(Math.max(0, filesToRemove.size() - property.getCount()))
                .forEach(path -> {
                    try {
                        UtilFile.remove(path);
                    } catch (Exception e) {
                        App.error(e);
                    }
                });
    }

    // Вызывается когда меняется X файл, так как он достиг максимального размера
    public void onDataFileSwap(String fileName, AsyncFileWriterRolling<BlockData<T>> xAsyncFileWriterRolling) {
        // Если последний зарегистрированный Rider существует и ещё жив - оповестим, что запись закончена
        ManagerConfiguration<Rider> lastRiderConfiguration = queueRiderConfiguration.peekLast();
        if (lastRiderConfiguration != null && lastRiderConfiguration.isAlive()) {
            lastRiderConfiguration.get().getQueueRetry().setFinishState(true);
        }
        // Первым делом надо создать .commit файл, что бы если произойдёт рестарт, мы могли понять, что файл ещё не
        // обработан, так как после обработки файл удаляется
        String filePath = property.getDirectory() + "/" + fileName;
        try {
            UtilFile.createNewFile(filePathDataToControl(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ManagerConfiguration<Rider> newRiderConfiguration = getRiderConfiguration(filePath, false);
        // Запускаем сразу контроллер коммитов, что бы onBinWrite мог в него уже передавать записанные position
        if (newRiderConfiguration != null) {
            newRiderConfiguration.get(); // Это обязательно! Поэтому буду дублировать, что бы по случайности не удалить
            // Добавляю зависимость опускания, что бы Y закрывался позже чем X
            newRiderConfiguration.get().getListShutdownAfter().add(xAsyncFileWriterRolling);
        }
    }

    private ManagerConfiguration<Rider> getRiderConfiguration(String filePathData, boolean fileXFinishState) {
        // Обязательно должен существовать commit файл, если пакет данных не обработан, после обработки commit
        // файл удаляется, если нет commit - то смысла в этом больше нет
        if (!Files.exists(Paths.get(filePathDataToControl(filePathData)))) {
            App.error(new ForwardException(new HashMapBuilder<>()
                    .append("filePathData", filePathData)
                    .append("fileXFinishState", fileXFinishState)
            ));
            return null;
        }
        return mapRiderConfiguration.computeIfAbsent(
                filePathData, // Нам тут нужна ссылка на X так как BrokerPersistElement.getFilePath возвращает именно его
                _ -> {
                    ManagerConfiguration<Rider> riderManagerConfiguration = ManagerConfiguration.getInstance(
                            getCascadeKey(ns),
                            java.util.UUID.randomUUID().toString(),
                            Rider.class,
                            rider -> {
                                // Каждый блок записи list<Y> может быть последним, так как будут обработаны все X
                                rider.setup(filePathData, property, this::removeRiderIfComplete, fileXFinishState);
                            }
                    );
                    queueRiderConfiguration.add(riderManagerConfiguration);
                    return riderManagerConfiguration;
                });
    }

    public static String filePathDataToControl(String filePathData) {
        return filePathData + ".control";
    }

    public static String filePathControlToData(String filePathControl) {
        return filePathControl.substring(0, filePathControl.length() - 8);
    }

    public boolean isEmpty() {
        return mapRiderConfiguration.isEmpty();
    }

    public PropertyDispatcher<Integer> getPropertyDispatcher() {
        return null;
    }

    @Override
    public void runOperation() {
        if (restoreElementFromByte == null) {
            throw new RuntimeException("restoreElementFromByte is null");
        }
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
        // Если не закрыть xWriterConfiguration - то Rider не получит уведомление, что файл закрылся на запись, а это
        // значит, что мы не сможем получить isProcessed = true. А может нам и не надо его получит? При следующем старте
        // всё удалится.
        // Все смежные ресурсы будут выключены в Manager
    }

    public void add(@NotNull T element) throws Exception {
        tpsEnqueue.incrementAndGet();
        writerManagerConfiguration.get().writeAsync(new BlockData<>(element));
    }

    public void commit(BlockData<T> element) {
        // К моменту commit уже должна быть конфигурация Rider
        ManagerConfiguration<Rider> riderConfiguration = element.getRiderConfiguration();
        if (riderConfiguration == null) {
            throw new RuntimeException("Rider is null");
        }
        riderConfiguration.get().onCommitData(element);
    }

    public record LastDataWrite(DataReadWrite dataReadWrite, ManagerConfiguration<Rider> riderConfiguration) {}

    private LastDataWrite getLastDataWrite() {
        // Так как последний Rider, при нагрузке, всегда будет находиться в finishStatus = false,
        // мы должны перебирать всех Rider с конца очереди queueRiderConfiguration в поиске LastDataWrite
        for (Iterator<ManagerConfiguration<Rider>> it = queueRiderConfiguration.descendingIterator(); it.hasNext(); ) {
            ManagerConfiguration<Rider> riderConfig = it.next();
            try {
                // Может такое случится, что Rider не сможет стартануть из-за того, что файла на FS не будет
                DataReadWrite dataReadWrite = riderConfig
                        .get()
                        .getQueueRetry()
                        .pollLast(property.getRetryTimeoutMs());
                if (dataReadWrite != null) {
                    return new LastDataWrite(dataReadWrite, riderConfig);
                }
            } catch (Throwable th) {
                App.error(th);
            }
        }
        return null;
    }

    public BlockData<T> poll() {
        LastDataWrite lastDataWrite = getLastDataWrite();
        if (lastDataWrite == null) {
            return null;
        }

        tpsDequeue.incrementAndGet();
        DataReadWrite dataReadWrite = lastDataWrite.dataReadWrite();
        Object object = dataReadWrite.getObject();
        if (object != null) {
            @SuppressWarnings("unchecked")
            BlockData<T> cast = (BlockData<T>) object;
            return cast;
        }
        BlockData<T> tx = new BlockData<>(restoreElementFromByte.apply(dataReadWrite.getBytes()));
        tx.setPosition(dataReadWrite.getPosition());
        tx.setRiderConfiguration(lastDataWrite.riderConfiguration());
        return tx;
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (key.equals("directory")) {
            dataWriterInit();
        }
    }

    public ManagerConfiguration<Rider> getLastRiderConfiguration() {
        return queueRiderConfiguration.peekLast();
    }

    @Override
    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of(new StatisticDataHeader(getClass(), ns)
                .addHeader("enq", tpsEnqueue.getAndSet(0))
                .addHeader("deq", tpsDequeue.getAndSet(0))
                .addHeader("sizeRider", mapRiderConfiguration.size())
        );
    }

}
