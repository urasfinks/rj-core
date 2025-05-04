package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.broker.BrokerRepositoryProperty;
import ru.jamsys.core.extension.property.PropertyDispatcher;

import java.util.List;
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

    private final BrokerRepositoryProperty propertyBroker = new BrokerRepositoryProperty();

    private final PropertyDispatcher<Integer> propertyDispatcher;

    public BrokerPersist(String ns, ApplicationContext applicationContext) {
        this.ns = ns;
        this.applicationContext = applicationContext;

        propertyDispatcher = new PropertyDispatcher<>(
                applicationContext.getBean(ServiceProperty.class),
                null,
                getPropertyBroker(),
                getCascadeKey(ns)
        );
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
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

    public void add(T x) {

    }

    @Override
    public void commit(T element, String groupRead) {

    }

}
