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
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
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

    private final BrokerPersistRepositoryProperty property = new BrokerPersistRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicInteger tpsEnqueue = new AtomicInteger(0);

    private ManagerConfiguration<AsyncFileWriterRolling<X<T>>> xWriterConfiguration;

    // Конфиг может быть удалён, только если файл полностью обработан, до этого момента он должен быть тут 100%
    private final Map<String, ManagerConfiguration<Rider>> mapRiderConfiguration = new ConcurrentHashMap<>();

    // Для того, что бы наполнять queue надо брать существующие CommitController по порядку и доить их
    private final ConcurrentLinkedDeque<ManagerConfiguration<Rider>> queueRiderConfiguration = new ConcurrentLinkedDeque<>();

    private Function<byte[], T> restoreElementFromByte;

    public BrokerPersist(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                property,
                getCascadeKey(ns)
        );
        xWriterInit();
    }

    public void setupRestoreElementFromByte(Function<byte[], T> restoreElementFromByte){
        this.restoreElementFromByte = restoreElementFromByte;
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                //.append("brokerPersistRepositoryProperty", property)
                ;
    }

    public void xWriterInit() {
        String key = "X";
        if (xWriterConfiguration != null) {
            App.get(Manager.class).remove(AsyncFileWriterRolling.class, key, getCascadeKey(ns));
        }
        xWriterConfiguration = ManagerConfiguration.getInstance(
                AsyncFileWriterRolling.class,
                key,
                getCascadeKey(ns),
                managerElement -> {
                    managerElement.setupRepositoryProperty(property);
                    managerElement.setupOnFileSwap(this::onXFileSwap);
                    managerElement.setupOnWrite(this::onXWrite);
                    // Если по каким-то причинам закрывается файл данных, надо оповестить rider
                    // При обычной работе происходит просто onSwap и едем дальше, а при завершении работы приложения
                    // или просто BrokerPersist отъехал от дел, буду закрываться ресурсы, вот тут тоже вызовется
                    managerElement.getListOnPostShutdown().add(() -> {
                        ManagerConfiguration<Rider> riderConfiguration = mapRiderConfiguration
                                .get(managerElement.getFilePath());
                        if (riderConfiguration != null && riderConfiguration.isAlive()) {
                            riderConfiguration.get().getQueueRetry().setFinishState(true);
                        }
                    });
                }
        );

        // При старте мы должны поднять все CommitController в карту commitControllers это надо, что бы наполнять
        // очередь, так как она цикличная
        UtilFile
                .getFilesRecursive(property.getDirectory())
                .stream()
                .filter(s -> s.endsWith(".commit"))
                .sorted()
                .toList()
                .forEach(filePathY -> {
                    String relativePath = UtilFile.getRelativePath(
                            property.getDirectory(),
                            filePathYToX(filePathY)
                    );
                    getRiderConfiguration(relativePath, true);
                });
    }

    // Вызывается из планировщика выполняющего запись в файл (однопоточное использование)
    public void onXWrite(String filePath, List<X<T>> listX) {
        ManagerConfiguration<Rider> riderConfiguration = mapRiderConfiguration.get(filePath);
        if (riderConfiguration == null) {
            throw new RuntimeException("Rider(" + filePath + ") not found");
        }
        // Бывает такое, что конфигурации останавливаются, из-за того, что не используются. Используются, это когда
        // в них коммитят позиции, извлекают из них не закоммиченные позиции
        riderConfiguration.executeIfAlive(rider -> {
            for (X<T> x : listX) {
                x.setRiderConfiguration(riderConfiguration);
                rider.onWriteX(x);
            }
        });
    }

    // Вызывается из планировщика выполняющего запись в файл (однопоточное использование)
    private void removeRiderIfComplete(Rider rider) {
        if (rider.getQueueRetry().isProcessed()) {
            // rider.shutdown(); // Когда менеджер будет его выбрасывать, сам выключит
            ManagerConfiguration<Rider> removedRiderConfiguration
                    = mapRiderConfiguration.remove(filePathYToX(rider.getFilePathY()));
            // Если контроллер найден по имени файла, удалим и из очереди
            if (removedRiderConfiguration != null) {
                queueRiderConfiguration.remove(removedRiderConfiguration);
            }
        }
    }

    // Вызывается когда меняется X файл, так как он достиг максимального размера
    public void onXFileSwap(String fileName, AsyncFileWriterRolling<X<T>> xAsyncFileWriterRolling) {
        // Если последний зарегистрированный Rider существует и ещё жив - оповестим, что запись закончена
        ManagerConfiguration<Rider> lastRiderConfiguration = queueRiderConfiguration.peekLast();
        if (lastRiderConfiguration != null && lastRiderConfiguration.isAlive()) {
            lastRiderConfiguration.get().getQueueRetry().setFinishState(true);
        }
        // Первым делом надо создать .commit файл, что бы если произойдёт рестарт, мы могли понять, что файл ещё не
        // обработан, так как после обработки файл удаляется
        String filePath = property.getDirectory() + "/" + fileName;
        try {
            UtilFile.createNewFile(filePathXToY(filePath));
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

    private ManagerConfiguration<Rider> getRiderConfiguration(String filePathX, boolean fileXFinishState) {
        // Обязательно должен существовать commit файл, если пакет данных не обработан, после обработки commit
        // файл удаляется, если нет commit - то смысла в этом больше нет
        if (!Files.exists(Paths.get(filePathXToY(filePathX)))) {
            return null;
        }
        return mapRiderConfiguration.computeIfAbsent(
                filePathX, // Нам тут нужна ссылка на X так как BrokerPersistElement.getFilePath возвращает именно его
                _ -> {
                    ManagerConfiguration<Rider> riderManagerConfiguration = ManagerConfiguration.getInstance(
                            Rider.class,
                            java.util.UUID.randomUUID().toString(),
                            filePathX,
                            rider -> {
                                // Каждый блок записи list<Y> может быть последним, так как будут обработаны все X
                                rider.setup(property, this::removeRiderIfComplete, fileXFinishState);
                            }
                    );
                    queueRiderConfiguration.add(riderManagerConfiguration);
                    return riderManagerConfiguration;
                });
    }

    public static String filePathXToY(String filePathX) {
        return filePathX + ".commit";
    }

    public static String filePathYToX(String filePathY) {
        return filePathY.substring(0, filePathY.length() - 7);
    }

    public boolean isEmpty() {
        return mapRiderConfiguration.isEmpty();
    }

    public PropertyDispatcher<Integer> getPropertyDispatcher() {
        return null;
    }

    @Override
    public void runOperation() {
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

    public void add(@NotNull T element) {
        tpsEnqueue.incrementAndGet();
        xWriterConfiguration.get().writeAsync(new X<>(element));
    }

    public void commit(X<T> element) {
        // К моменту commit уже должна быть конфигурация Rider
        ManagerConfiguration<Rider> riderConfiguration = element.getRiderConfiguration();
        if (riderConfiguration == null) {
            throw new RuntimeException("Rider is null");
        }
        riderConfiguration.get().onCommitX(element);
    }

    public record LastDataWrite(DataReadWrite dataReadWrite, ManagerConfiguration<Rider> riderConfiguration) {}

    private LastDataWrite getLastDataWrite() {
        // Так как последний Rider, при нагрузке, всегда будет находиться в finishStatus = false,
        // мы должны перебирать всех Rider с конца очереди queueRiderConfiguration в поиске LastDataWrite
        for (Iterator<ManagerConfiguration<Rider>> it = queueRiderConfiguration.descendingIterator(); it.hasNext(); ) {
            ManagerConfiguration<Rider> riderConfig = it.next();
            DataReadWrite dataReadWrite = riderConfig
                    .get()
                    .getQueueRetry()
                    .pollLast(property.getRetryTimeoutMs());
            if (dataReadWrite != null) {
                return new LastDataWrite(dataReadWrite, riderConfig);
            }
        }
        return null;
    }

    public X<T> poll() {
        LastDataWrite lastDataWrite = getLastDataWrite();
        if (lastDataWrite == null) {
            return null;
        }

        tpsDequeue.incrementAndGet();
        DataReadWrite dataReadWrite = lastDataWrite.dataReadWrite();
        Object object = dataReadWrite.getObject();
        if (object != null) {
            @SuppressWarnings("unchecked")
            X<T> cast = (X<T>) object;
            return cast;
        }
        X<T> tx = new X<>(restoreElementFromByte.apply(dataReadWrite.getBytes()));
        tx.setPosition(dataReadWrite.getPosition());
        tx.setRiderConfiguration(lastDataWrite.riderConfiguration());
        return tx;
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (key.equals("directory")) {
            xWriterInit();
        }
    }

    public ManagerConfiguration<Rider> getLastRiderConfiguration() {
        return queueRiderConfiguration.peekLast();
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of(new DataHeader()
                .setBody(getCascadeKey(ns))
                .addHeader("tpsEnqueue", tpsEnqueue.getAndSet(0))
                .addHeader("tpsDequeue", tpsDequeue.getAndSet(0))
                .addHeader("sizeRider", mapRiderConfiguration.size())
        );
    }

}
