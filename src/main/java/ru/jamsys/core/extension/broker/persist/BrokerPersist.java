package ru.jamsys.core.extension.broker.persist;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.AbstractAsyncFileWriter;
import ru.jamsys.core.extension.async.writer.AsyncFileWriterRolling;
import ru.jamsys.core.extension.async.writer.DataReadWrite;
import ru.jamsys.core.extension.broker.Broker;
import ru.jamsys.core.extension.broker.BrokerPersistRepositoryProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
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
@SuppressWarnings("unused")
public class BrokerPersist<T extends ByteSerializable>
        extends AbstractManagerElement
        implements PropertyListener, Broker {

    private final String ns;

    private final ApplicationContext applicationContext;

    private final BrokerPersistRepositoryProperty property = new BrokerPersistRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicInteger tpsEnqueue = new AtomicInteger(0);

    private Manager.Configuration<AbstractAsyncFileWriter<X<T>>> xWriterConfiguration;

    // Конфиг может быть удалён, только если файл полностью обработан, до этого момента он должен быть тут 100%
    private final Map<String, Manager.Configuration<Rider>> mapRiderConfiguration = new ConcurrentHashMap<>();

    // Для того, что бы наполнять queue надо брать существующие CommitController по порядку и доить их
    private final ConcurrentLinkedDeque<Manager.Configuration<Rider>> queueRiderConfiguration = new ConcurrentLinkedDeque<>();

    private final Function<byte[], T> restoreElementFromByte;

    public BrokerPersist(
            String ns,
            ApplicationContext applicationContext,
            Function<byte[], T> restoreElementFromByte
    ) {
        this.ns = ns;
        this.applicationContext = applicationContext;
        this.restoreElementFromByte = restoreElementFromByte;

        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class, applicationContext),
                this,
                property,
                getCascadeKey(ns)
        );

        xWriterInit();
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
        String key = getCascadeKey(ns, "bin");
        if (xWriterConfiguration != null) {
            App.get(Manager.class, applicationContext).remove(AbstractAsyncFileWriter.class, key);
        }
        xWriterConfiguration = App.get(Manager.class, applicationContext).configureGeneric(
                AbstractAsyncFileWriter.class,
                // ns уникально в пределах BrokerPersist, но нам надо больше уникальности, так как у нас несколько
                // AbstractAsyncFileWriter.class (Rolling/Wal)
                key,
                ns1 -> {
                    AsyncFileWriterRolling<X<T>> xAsyncFileWriterRolling = new AsyncFileWriterRolling<>(
                            property,
                            this::onXWrite,
                            this::onXFileSwap
                    );
                    // Если по каким-то причинам закрывается файл данных, надо оповестить rider
                    // При обычной работе происходит просто onSwap и едем дальше, а при завершении работы приложения
                    // или просто BrokerPersist отъехал от дел, буду закрываться ресурсы, вот тут тоже вызовется
                    xAsyncFileWriterRolling.getListOnPostShutdown().add(() -> {
                        Manager.Configuration<Rider> riderConfiguration = mapRiderConfiguration
                                .get(xAsyncFileWriterRolling.getFilePath());
                        if (riderConfiguration != null && riderConfiguration.isAlive()) {
                            riderConfiguration.get().getQueueRetry().setFinishState(true);
                        }
                    });
                    return xAsyncFileWriterRolling;
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
        Manager.Configuration<Rider> riderConfiguration = mapRiderConfiguration.get(filePath);
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
            Manager.Configuration<Rider> removedRiderConfiguration
                    = mapRiderConfiguration.remove(filePathYToX(rider.getFilePathY()));
            // Если контроллер найден по имени файла, удалим и из очереди
            if (removedRiderConfiguration != null) {
                queueRiderConfiguration.remove(removedRiderConfiguration);
            }
        }
    }

    // Вызывается когда меняется bin файл, так как он достиг максимального размера
    public void onXFileSwap(String fileName, AsyncFileWriterRolling<X<T>> xAsyncFileWriterRolling) {
        // Если последний зарегистрированный Rider существует и ещё жив - оповестим, что запись закончена
        Manager.Configuration<Rider> lastRiderConfiguration = queueRiderConfiguration.peekLast();
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
        Manager.Configuration<Rider> newRiderConfiguration = getRiderConfiguration(filePath, false);
        // Запускаем сразу контроллер коммитов, что бы onBinWrite мог в него уже передавать записанные position
        if (newRiderConfiguration != null) {
            newRiderConfiguration.get(); // Это обязательно! Поэтому буду дублировать, что бы по случайности не удалить
            // Добавляю зависимость опускания, что бы Y закрывался позже чем X
            newRiderConfiguration.get().getListShutdownAfter().add(xAsyncFileWriterRolling);
        }
    }

    private Manager.Configuration<Rider> getRiderConfiguration(String filePathX, boolean fileXFinishState) {
        // Обязательно должен существовать commit файл, если пакет данных не обработан, после обработки commit
        // файл удаляется, если нет commit - то смысла в этом больше нет
        if (!Files.exists(Paths.get(filePathXToY(filePathX)))) {
            return null;
        }
        return mapRiderConfiguration.computeIfAbsent(
                filePathX, // Нам тут нужна ссылка на bin так как BrokerPersistElement.getFilePath возвращает именно его
                _ -> {
                    Manager.Configuration<Rider> configure = App.get(Manager.class, applicationContext).configure(
                            Rider.class,
                            getCascadeKey(ns, filePathX), // Тут главно, что бы просто было уникальным
                            ns1 -> new Rider(
                                    property,
                                    ns1,
                                    filePathX,
                                    fileXFinishState,
                                    // Каждый блок записи list<Y> может быть последним, так как будут обработаны все X
                                    this::removeRiderIfComplete
                            )
                    );
                    queueRiderConfiguration.add(configure);
                    return configure;
                });
    }

    public static String filePathXToY(String filePathX) {
        return filePathX + ".commit";
    }

    public static String filePathYToX(String filePathY) {
        return filePathY.substring(0, filePathY.length() - 7);
    }

    @Override
    public long size() {
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return mapRiderConfiguration.isEmpty();
    }

    @Override
    public int getOccupancyPercentage() {
        return 0;
    }

    @Override
    public PropertyDispatcher<Integer> getPropertyDispatcher() {
        return null;
    }

    @Override
    public void reset() {
        mapRiderConfiguration.clear();
        queueRiderConfiguration.clear();
        tpsEnqueue.set(0);
        tpsDequeue.set(0);
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
        Manager.Configuration<Rider> riderConfiguration = element.getRiderConfiguration();
        if (riderConfiguration == null) {
            throw new RuntimeException("Rider is null");
        }
        riderConfiguration.get().onCommitX(element);
    }

    public record LastDataWrite(DataReadWrite dataReadWrite, Manager.Configuration<Rider> riderConfiguration) {}

    private LastDataWrite getLastDataWrite() {
        // Так как последний Rider, при нагрузке, всегда будет находиться в finishStatus = false,
        // мы должны перебирать всех Rider с конца очереди queueRiderConfiguration в поиске LastDataWrite
        for (Iterator<Manager.Configuration<Rider>> it = queueRiderConfiguration.descendingIterator(); it.hasNext(); ) {
            Manager.Configuration<Rider> riderConfig = it.next();
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

    public Manager.Configuration<Rider> getLastRiderConfiguration() {
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
