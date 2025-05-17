package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriter;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterRolling;
import ru.jamsys.core.extension.batch.writer.DataPayload;
import ru.jamsys.core.extension.batch.writer.Position;
import ru.jamsys.core.extension.broker.Broker;
import ru.jamsys.core.extension.broker.BrokerPersistRepositoryProperty;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
// корреткно)

@Getter
@SuppressWarnings("unused")
public class BrokerPersist<T extends Position & ByteSerializable>
        extends AbstractManagerElement
        implements PropertyListener, Broker {

    private final String ns;

    private final ApplicationContext applicationContext;

    private final BrokerPersistRepositoryProperty propertyBroker = new BrokerPersistRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final ConcurrentLinkedDeque<X<T>> queue = new ConcurrentLinkedDeque<>();

    private final AtomicInteger queueSize = new AtomicInteger(0);

    private Manager.Configuration<AbstractAsyncFileWriter<X<T>>> xWriterConfiguration;

    // Конфиг может быть удалён, только если файл полностью обработан, до этого момента он должен быть тут 100%
    private final Map<String, Manager.Configuration<Rider>> mapRiderConfiguration = new ConcurrentHashMap<>();

    // Для того, что бы наполнять queue надо брать существующие CommitController по порядку и доить их
    private final ConcurrentLinkedDeque<Manager.Configuration<Rider>> queueRiderConfiguration = new ConcurrentLinkedDeque<>();

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final Function<byte[], T> byteDeserialize;

    public BrokerPersist(
            String ns,
            ApplicationContext applicationContext,
            Function<byte[], T> byteDeserialize
    ) {
        this.ns = ns;
        this.applicationContext = applicationContext;
        this.byteDeserialize = byteDeserialize;

        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class, applicationContext),
                this,
                propertyBroker,
                getCascadeKey(ns)
        );

        xWriterInit();
    }

    @Override
    public void helper() {
        // Если очередь стала меньше минимальной планки по заполнению
        if (queueSize.get() < propertyBroker.getFillThresholdMin()) {
            // Крутим пока размер не достигнет верхнего порога
            while (queueSize.get() < propertyBroker.getFillThresholdMax()) {
                // Подбираем последний и высасываем его до конца, если он вообще существует
                Manager.Configuration<Rider> riderConfiguration = queueRiderConfiguration.peekLast();
                if (riderConfiguration == null) {
                    break;
                }
                Rider rider = riderConfiguration.get();
                if (rider.getQueueRetry().isProcessed()) {
                    removeRiderIfComplete(rider);
                    continue;
                }
                // Rider не завершён, но и на обработку нет ничего, так как выданы на обработку элементы и не закоммичены
                if (rider.getQueueRetry().parkIsEmpty()) {
                    break;
                }
                DataPayload dataPayload = rider.getQueueRetry().pollLast(propertyBroker.getRetryTimeoutMs());
                if (dataPayload == null) {
                    continue;
                }
                Object object = dataPayload.getObject();
                if (object != null) {
                    @SuppressWarnings("unchecked")
                    X<T> cast = (X<T>) object;
                    queue.add(cast);
                    continue;
                }
                X<T> tx = new X<>(byteDeserialize.apply(dataPayload.getBytes()));
                tx.setPosition(dataPayload.getPosition());
                tx.setRiderConfiguration(riderConfiguration);
                queue.add(tx);
                queueSize.incrementAndGet();
            }
        }
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
                            applicationContext,
                            ns1,
                            propertyBroker.getDirectory(),
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
                .getFilesRecursive(propertyBroker.getDirectory())
                .stream()
                .filter(s -> s.endsWith(".commit"))
                .sorted()
                .toList()
                .forEach(filePathY -> {
                    String relativePath = UtilFile.getRelativePath(
                            propertyBroker.getDirectory(),
                            filePathYToX(filePathY)
                    );
                    getRiderConfiguration(relativePath, true);
                });
    }

    // Вызывается из планировщика выполняющего запись в файл (однопоточное использование)
    public void onXWrite(String filePath, List<X<T>> listX) {
        int maxSize = propertyBroker.getSize();

        if (listX.size() > maxSize) {
            // Берём только последние maxSize элементов
            int fromIndex = listX.size() - maxSize;
            List<X<T>> tail = listX.subList(fromIndex, listX.size());

            queue.clear();
            queue.addAll(tail);
            queueSize.set(tail.size()); // Обновляем после добавления
        } else {
            queue.addAll(listX);
            queueSize.addAndGet(listX.size());

            // Удаление из начала очереди при переполнении
            while (queueSize.get() > maxSize) {
                if (queue.pollFirst() != null) {
                    queueSize.decrementAndGet();
                } else {
                    break;
                }
            }
        }

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
    public void onXFileSwap(String fileName) {
        // Если последний зарегистрированный Rider существует и ещё жив - оповестим, что запись закончена
        Manager.Configuration<Rider> lastRiderConfiguration = queueRiderConfiguration.peekLast();
        if (lastRiderConfiguration != null && lastRiderConfiguration.isAlive()) {
            lastRiderConfiguration.get().getQueueRetry().setFinishState(true);
        }
        // Первым делом надо создать .commit файл, что бы если произойдёт рестарт, мы могли понять, что файл ещё не
        // обработан, так как после обработки файл удаляется
        String filePath = propertyBroker.getDirectory() + "/" + fileName;
        try {
            UtilFile.createNewFile(filePathXToY(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Manager.Configuration<Rider> newRiderConfiguration = getRiderConfiguration(filePath, false);
        // Запускаем сразу контроллер коммитов, что бы onBinWrite мог в него уже передавать записанные position
        if (newRiderConfiguration != null) {
            newRiderConfiguration.get();
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
                            key1 -> new Rider(
                                    applicationContext,
                                    key1,
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
        return queueSize.get();
    }

    @Override
    public boolean isEmpty() {
        // нет данных в очереди на обработку
        // все rider завершены
        return queue.isEmpty() && mapRiderConfiguration.isEmpty();
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
        queue.clear();
        queueSize.set(0);
        mapRiderConfiguration.clear();
        queueRiderConfiguration.clear();
        tpsDequeue.set(0);
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
        // Все смежные ресурсы будут выключены в Manager
    }

    public void add(@NotNull T element) {
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

    public X<T> poll() {
        // Очередь цикличная,
        X<T> poll = queue.poll();
        if (poll != null) {
            tpsDequeue.incrementAndGet();
            queueSize.decrementAndGet();
        }
        return poll;
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
        int tpsDequeueFlush = tpsDequeue.getAndSet(0);
        return List.of(new DataHeader()
                .setBody(getCascadeKey(ns))
                .addHeader("tpsDeq", tpsDequeueFlush)
                .addHeader("size", queueSize.get())
        );
    }

}
