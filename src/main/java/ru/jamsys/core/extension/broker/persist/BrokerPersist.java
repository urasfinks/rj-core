package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriter;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterRolling;
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

// SSD 80K IOPS при 4KB на блок = 320 MB/s на диске в режиме RandomAccess и до 550 MB/s в линейной записи (секвентальная)
// Для гарантии записи надо использовать StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DSYNC
// (в таком режиме atime, mtime, размер файла может быть не синхранизован, но данные при восстановлении будут вычитаны
// корреткно)

@Getter
@SuppressWarnings("unused")
public class BrokerPersist<T extends ByteSerialization>
        extends AbstractBrokerPersist<T>
        implements PropertyListener {

    private final String ns;

    private final ApplicationContext applicationContext;

    private final BrokerPersistRepositoryProperty propertyBroker = new BrokerPersistRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final ConcurrentLinkedDeque<BrokerPersistElement<T>> queue = new ConcurrentLinkedDeque<>();

    private final AtomicInteger queueSize = new AtomicInteger(0);

    private Manager.Configuration<AbstractAsyncFileWriter<BrokerPersistElement<T>>> binConfiguration;

    // Конфиг может быть удалён, только если файл полностью обработан, до этого момента он должен быть тут 100%
    private final Map<String, Manager.Configuration<CommitController>> mapCommitControllerConfiguration = new ConcurrentHashMap<>();

    // Для того, что бы наполнять queue надо брать существующие CommitController по порядку и доить их
    private final ConcurrentLinkedDeque<Manager.Configuration<CommitController>> queueCommitControllerConfiguration = new ConcurrentLinkedDeque<>();

    // Последний зарегистрированный конфиг Rider, что бы можно было оповещать о новых поступлениях данных position
    private Manager.Configuration<CommitController> lastCommitConfiguration;

    public BrokerPersist(String ns, ApplicationContext applicationContext) {
        this.ns = ns;
        this.applicationContext = applicationContext;

        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class, applicationContext),
                this,
                propertyBroker,
                getCascadeKey(ns)
        );

        binWriteConfiguration();
    }

    @Override
    public void helper() {
        if (queueSize.get() < propertyBroker.getFillThresholdMin()) {
            Manager.Configuration<CommitController> commitControllerConfiguration
                    = queueCommitControllerConfiguration.pollLast();
            if (commitControllerConfiguration != null) {
                CommitController commitController = commitControllerConfiguration.get();

            }
            //commitControllers.
        }
    }

    public void binWriteConfiguration() {
        String key = getCascadeKey(ns, "bin");
        if (binConfiguration != null) {
            App.get(Manager.class, applicationContext).remove(AbstractAsyncFileWriter.class, key);
        }
        binConfiguration = App.get(Manager.class, applicationContext).configureGeneric(
                AbstractAsyncFileWriter.class,
                // ns уникально в пределах BrokerPersist, но нам надо больше уникальности, так как у нас несколько
                // AbstractAsyncFileWriter.class (Rolling/Wal)
                key,
                ns1 -> new AsyncFileWriterRolling<>(
                        applicationContext,
                        ns1,
                        propertyBroker.getDirectory(),
                        this::onBinWrite,
                        this::onBinSwap
                )
        );
        // При старте мы должны поднять все CommitController в карту commitControllers это надо, что бы наполнять
        // очередь, так как она цикличная
        UtilFile
                .getFilesRecursive(propertyBroker.getDirectory())
                .stream()
                .filter(s -> s.endsWith(".commit"))
                .toList()
                .forEach(filePathCommit -> {
                    // Отрезаем .commit так как CommitController работает с оригинальными путями, что бы их
                    computeIfAbsentCommitController(commitToBin(filePathCommit));
                });
    }

    // Вызывается из планировщика выполняющего запись в файл (однопоточное использование)
    public void onBinWrite(List<BrokerPersistElement<T>> brokerPersistElements) {
        int maxSize = propertyBroker.getSize();

        if (brokerPersistElements.size() > maxSize) {
            // Берём только последние maxSize элементов
            int fromIndex = brokerPersistElements.size() - maxSize;
            List<BrokerPersistElement<T>> tail = brokerPersistElements.subList(fromIndex, brokerPersistElements.size());

            queue.clear();
            queue.addAll(tail);
            queueSize.set(tail.size()); // Обновляем после добавления
        } else {
            queue.addAll(brokerPersistElements);
            queueSize.addAndGet(brokerPersistElements.size());

            // Удаление из начала очереди при переполнении
            while (queueSize.get() > maxSize) {
                if (queue.pollFirst() != null) {
                    queueSize.decrementAndGet();
                } else {
                    break;
                }
            }
        }

        // Работа с CommitController
        BrokerPersistElement<T> first = brokerPersistElements.getFirst();
        Manager.Configuration<CommitController> commitControllerConfiguration = mapCommitControllerConfiguration.get(first.getFilePath());
        if (commitControllerConfiguration == null) {
            throw new RuntimeException("CommitController(" + first.getFilePath() + ") not found");
        }
        // Бывает такое, что конфигурации останавливаются, из-за того, что не используются. Используются, это когда
        // в них коммитят позиции, извлекают из них не закоммиченные позиции
        if (!commitControllerConfiguration.isAlive()) {
            return;
        }
        CommitController commitController = commitControllerConfiguration.get();
        if (commitController != null) {
            commitController.add(brokerPersistElements);
        }
    }

    // Вызывается из планировщика выполняющего запись в файл (однопоточное использование)
    public void onCommitWrite(CommitController commitController) {
        if (commitController.isComplete()) {
            // rider.shutdown(); // Когда менеджер будет его выбрасывать, сам выключит
            Manager.Configuration<CommitController> removedCommitControllerConfiguration
                    = mapCommitControllerConfiguration.remove(commitToBin(commitController.getFilePathCommit()));
            // Если контроллер найден по имени файла, удалим и из очереди
            if (removedCommitControllerConfiguration != null) {
                queueCommitControllerConfiguration.remove(removedCommitControllerConfiguration);
            }
        }
    }

    // Вызывается когда меняется bin файл, так как он достиг максимального размера
    public void onBinSwap(String fileName) {
        // Если последний зарегистрированный Rider существует и ещё жив - оповестим, что запись закончена
        if (lastCommitConfiguration != null && lastCommitConfiguration.isAlive()) {
            lastCommitConfiguration.get().getBinFileReaderResult().setFinishState(true);
        }
        // Первым делом надо создать .commit файл, что бы если произойдёт рестарт, мы могли понять, что файл ещё не
        // обработан, так как после обработки файл удаляется
        String filePath = propertyBroker.getDirectory() + "/" + fileName;
        try {
            UtilFile.createNewFile(binToCommit(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lastCommitConfiguration = computeIfAbsentCommitController(filePath);
        // Запускаем сразу контроллер коммитов, что бы onBinWrite мог в него уже передавать записанные position
        if (lastCommitConfiguration != null) {
            lastCommitConfiguration.get();
        }
    }

    private Manager.Configuration<CommitController> computeIfAbsentCommitController(String filePathBin) {
        // Обязательно должен существовать commit файл, если пакет данных не обработан, после обработки commit
        // файл удаляется, если нет commit - то смысла в этом больше нет
        if (!Files.exists(Paths.get(binToCommit(filePathBin)))) {
            return null;
        }
        return mapCommitControllerConfiguration.computeIfAbsent(
                filePathBin, // Нам тут нужна ссылка на bin так как BrokerPersistElement.getFilePath возвращает именно его
                _ -> {
                    Manager.Configuration<CommitController> configure = App.get(Manager.class, applicationContext).configure(
                            CommitController.class,
                            getCascadeKey(ns, filePathBin), // Тут главно, что бы просто было уникальным
                            ns1 -> {
                                try {
                                    // передаём fileName, так как Rider должен прочитать его
                                    return new CommitController(
                                            applicationContext,
                                            ns1,
                                            binToCommit(filePathBin),
                                            this::onCommitWrite
                                    );
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                    queueCommitControllerConfiguration.add(configure);
                    return configure;
                });
    }

    public static String binToCommit(String filePathBin) {
        return filePathBin + ".commit";
    }

    public static String commitToBin(String filePathCommit) {
        return filePathCommit.substring(0, filePathCommit.length() - 7);
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
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

    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
        if (binConfiguration.isAlive()) {
            binConfiguration.get().shutdown();
        }
        if (lastCommitConfiguration.isAlive()) {
            CommitController commitController = lastCommitConfiguration.get();
            commitController.getBinFileReaderResult().setFinishState(true);
            commitController.shutdown();
        }
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

    public void add(T element) {
        binConfiguration.get().writeAsync(new BrokerPersistElement<>(element));
    }

    @Override
    public void commit(BrokerPersistElement<T> element) {
        // К моменту commit уже должна быть конфигурация Rider
        Manager.Configuration<CommitController> commitControllerConfiguration = computeIfAbsentCommitController(element.getFilePath());
        if (commitControllerConfiguration == null) {
            throw new RuntimeException("CommitController(" + element.getFilePath() + ") not found");
        }
        commitControllerConfiguration.get().asyncWrite(element);
    }

    public BrokerPersistElement<T> poll() {
        // Очередь цикличная,
        return queue.poll();
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        binWriteConfiguration();
    }
}
