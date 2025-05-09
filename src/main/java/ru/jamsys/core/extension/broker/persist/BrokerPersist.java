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
import ru.jamsys.core.flat.util.UtilFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

// SSD 80K IOPS при 4KB на блок = 320 MB/s на диске в режиме RandomAccess и до 550 MB/s в линейной записи (секвентальная)
// Для гарантии записи надо использовать StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DSYNC
// (в таком режиме atime, mtime, размер файла может быть не синхранизован, но данные при восстановлении будут вычитаны
// корреткно)

// 1. Есть поставщик данных, он передаёт данные в BrokerPersist
// 2. Запись в log через AsyncFileWriter
// 3. onRead(log) -> Запись в wal AsyncFileWriterElement.position [position | status[INSERT/POLL/COMMIT]]
// 4. onRead(wal) -> пишем обработанную position
// 5. Внутренняя очередь может наполнится только тогда, когда все зарезервированные ранее позиции отчитались коммитом
// 6. Когда все элементы COMMIT - надо удалить log + wal и начать писать в другие файлы, что бы не занимать место



/*
* Пишем данные в log, в wal пишем только position обработанной записи.
* wal начинается с описания с какого position были считаны данные
* */


//3. Потом данные помещаются в SubscriberDataCommit (DSYNC запись в файл .wal) SubscriberDataCommit состоит из
//   списка DataPosition которые не закомичены как обработанные
//4. После того как данные будут сохранены на диске, мы проверим наличие очереди с short = 0 и если такая есть -
//   положим туда DataPosition
//5. В BrokerPersist может прийти подписчик с short id. Для этого id будет создана очередь и можно в многопоточном
//   режиме из этой очереди получать не прочитанные данные, записанные поставщиком. Если будет 2 подписчика, значит будет
//   2 очереди, значит данные, которые представляет поставщик - будут записаны в 2 очереди для разных подписчиков.
//6. Очереди подписчиков - это стандартный Broker, если за отведённое время данные не будут вычитаны - они будут
//   очищены. Что бы очереди наполнились - надо запросить данные. Получение данных из брокера будет не на прямую, а через
//   прокси, которое будет проверять пустоту и наполненность.
//7. При старте приложения - будет медленный старт (и у нас нет задачи быстрого старта), так как, что бы восстановить
//   SubscriberDataCommit - прийдётся прочитать весь .wal
//8. Когда в SubscriberDataCommit остаётся 0 не обработанных записей - .log и .wal удаляются
//9. Для систем с возможной дубликацией нужна специальная общая архитектура идемпотентности. Работа в режиме потери
//   (автокомит при изъятии) рассматривать вообще не будем в BrokerPersist (просто используйте BrokerMemory и всё).

@Getter
@SuppressWarnings("unused")
public class BrokerPersist<T extends ByteSerialization>
        extends AbstractBrokerPersist<T> {

    private final String ns;

    private final ApplicationContext applicationContext;

    private final BrokerPersistRepositoryProperty propertyBroker = new BrokerPersistRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final ConcurrentLinkedDeque<BrokerPersistElement<T>> queue = new ConcurrentLinkedDeque<>();

    private final Manager.Configuration<AbstractAsyncFileWriter<BrokerPersistElement<T>>> asyncFileWriterRollingConfiguration;

    // Конфиг может быть удалён, только если файл полностью обработан, до этого момента он должен быть тут 100%
    private final Map<String, Manager.Configuration<CommitController>> commitControllers = new ConcurrentHashMap<>();

    // Последний зарегистрированный конфиг Rider, что бы можно было оповещать о новых поступлениях данных position
    private Manager.Configuration<CommitController> lastCommitControllerConfiguration;

    public BrokerPersist(String ns, ApplicationContext applicationContext) {
        this.ns = ns;
        this.applicationContext = applicationContext;

        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class, applicationContext),
                null,
                getPropertyBroker(),
                getCascadeKey(ns)
        );

        asyncFileWriterRollingConfiguration = App.get(Manager.class, applicationContext).configureGeneric(
                AbstractAsyncFileWriter.class,
                // ns уникально в пределах BrokerPersist, но нам надо больше уникальности, так как у нас несколько
                // AbstractAsyncFileWriter.class (Rolling/Wal)
                getCascadeKey(ns, "main"),
                ns1 -> new AsyncFileWriterRolling<>(
                        applicationContext,
                        ns1,
                        propertyBroker.getDirectory(),
                        this::onMainWrite,
                        this::onSwap
                )
        );
    }

    public void onMainWrite(List<BrokerPersistElement<T>> brokerPersistElements) {
        queue.addAll(brokerPersistElements);
        // На момент записи не может быть перескакивания записи по разным файлам, то есть пачка содержит точно все блоки
        // принадлежащие одному файлу
        BrokerPersistElement<T> first = brokerPersistElements.getFirst();
        Manager.Configuration<CommitController> riderConfiguration = commitControllers.get(first.getFilePath());
        if (riderConfiguration == null) {
            throw new RuntimeException("Rider(" + first.getFilePath() + ") not found");
        }
        if (!riderConfiguration.isAlive()) {
            return;
        }
        CommitController commitController = riderConfiguration.get();
        if (commitController == null) {
            return;
        }
        commitController.addPosition(brokerPersistElements);
    }

    public void onSwap(String fileName) {
        // Если последний зарегистрированный Rider был и жив оповестим, что запись закончена
        if (lastCommitControllerConfiguration != null && lastCommitControllerConfiguration.isAlive()) {
            lastCommitControllerConfiguration.get().fileFinishWrite();
        }
        // Первым делом надо создать .wal файл, что бы если произойдёт рестарт, мы могли понять, что файл ещё не
        // обработан, так как после обработки файл удаляется
        String filePath = propertyBroker.getDirectory() + "/" + fileName;
        try {
            UtilFile.createNewFile(filePath + ".wal");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lastCommitControllerConfiguration = commitControllers.computeIfAbsent(
                filePath,
                _ -> App.get(Manager.class, applicationContext).configure(
                        CommitController.class,
                        getCascadeKey(ns, fileName),
                        ns1 -> {
                            try {
                                // передаём fileName, так как Rider должен прочитать
                                return new CommitController(
                                        applicationContext,
                                        ns1,
                                        filePath
                                );
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ));
        // Запускаем сразу контроллер коммитов, что бы onMainWrite мог в него уже передавать записанные position
        lastCommitControllerConfiguration.get();
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
        if (asyncFileWriterRollingConfiguration.isAlive()) {
            asyncFileWriterRollingConfiguration.get().shutdown();
        }
        if (lastCommitControllerConfiguration.isAlive()) {
            CommitController commitController = lastCommitControllerConfiguration.get();
            commitController.fileFinishWrite();
            commitController.shutdown();
        }
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

    public void add(T element) throws Throwable {
        asyncFileWriterRollingConfiguration.get().writeAsync(new BrokerPersistElement<>(element));
    }

    @Override
    public void commit(BrokerPersistElement<T> element) throws Throwable {
        // К моменту commit уже должна быть конфигурация Rider
        Manager.Configuration<CommitController> riderConfiguration = commitControllers.get(element.getFilePath());
        if (riderConfiguration == null) {
            throw new RuntimeException("CommitController(" + element.getFilePath() + ") not found");
        }
        CommitController commitController = riderConfiguration.get();
        commitController.asyncWrite(element.getPosition());
        if (commitController.isComplete()) {
            // rider.shutdown(); // Когда менеджер будет его выбрасывать, сам выключит
            commitControllers.remove(element.getFilePath());
        }
    }

    public BrokerPersistElement<T> poll() {
        return queue.poll();
    }

}
