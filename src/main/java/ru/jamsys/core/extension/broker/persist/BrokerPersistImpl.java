package ru.jamsys.core.extension.broker.persist;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.broker.Broker;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.function.Consumer;

//SSD 80K IOPS при 4KB на блок = 320 MB/s на диске в режиме RandomAccess и до 550 MB/s в линейной записи (секвентальная)
//Для гарантии записи надо использовать StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DSYNC (в таком режиме atime, mtime, размер файла может быть не синхранизован, но данные при восстановлении будут вычитаны корреткно)

//1. Есть поставщик данных, он передаёт данные в BrokerPersist
//2. Сначала данные записываются в .log. Данные собираются в пачки по N байт (что бы не расходовать IOPS) и записываются на файловую систему (Динамический размер батча - адаптация под нагрузку)
//3. Потом данные помещаются в SubscriberDataCommit (DSYNC запись в файл .wal) SubscriberDataCommit состоит из списка DataPosition которые не закомичены как обработанные
//4. После того как данные будут сохранены на диске, мы проверим наличие очереди с short = 0 и если такая есть - положим туда DataPosition
//5. В BrokerPersist может прийти подписчик с short id. Для этого id будет создана очередь и можно в многопоточном режиме из этой очереди получать не прочитанные данные, записанные поставщиком. Если будет 2 подписчика, значит будет 2 очереди, значит данные, которые представляет поставщик - будут записаны в 2 очереди для разных подписчиков.
//6. Очереди подписчиков - это стандартный Broker, если за отведённое время данные не будут вычитаны - они будут очищены. Что бы очереди наполнились - надо запросить данные. Получение данных из брокера будет не на прямую, а через прокси, которое будет проверять пустоту и наполненность.
//7. При старте приложения - будет медленный старт (и у нас нет задачи быстрого старта), так как, что бы восстановить SubscriberDataCommit - прийдётся прочитать весь .wal
//8. Когда в SubscriberDataCommit остаётся 0 не обработанных записей - .log и .wal удаляются
//9. Для систем с возможной дубликацией нужна специальная общая архитектура идемпотентности. Работа в режиме потери (автокомит при изъятии) рассматривать вообще не будем в BrokerPersist (просто используйте Broker и всё).

@SuppressWarnings("unused")
public class BrokerPersistImpl<T extends ByteSerialization>
        extends BrokerMemory<T>
        implements Broker<T>, BrokerPersist<T>,
        ManagerElement {

    public BrokerPersistImpl(
            String key,
            ApplicationContext applicationContext,
            Consumer<T> onDrop
    ) {
        super(key, applicationContext, onDrop);
    }

    @Override
    public ExpirationMsImmutableEnvelope<T> pollFirst(String groupRead) {
        return null;
    }

    @Override
    public ExpirationMsImmutableEnvelope<T> pollLast(String groupRead) {
        return null;
    }

    @Override
    public void commit(T element, String groupRead) {

    }

    @Override
    public DisposableExpirationMsImmutableEnvelope<T> add(ExpirationMsImmutableEnvelope<T> envelope) {
        return super.add(envelope);
    }

}
