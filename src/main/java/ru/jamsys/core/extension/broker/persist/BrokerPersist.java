package ru.jamsys.core.extension.broker.persist;

import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

public interface BrokerPersist<T extends ByteSerialization> extends Broker<T> {

    // Данные персистентного брокера могут читать в один момент времени сразу несколько групп
    // Сообщение реально вставляется одно, но его прочитать могут сразу несколько групп
    // по умолчанию

    ExpirationMsImmutableEnvelope<T> pollFirst(String groupRead);

    ExpirationMsImmutableEnvelope<T> pollLast(String groupRead);

    void commit(T element, String groupRead);

    default String getGroupReadOnWriteLog() { // Получить имя группы, в которую попадают данные записанные на ФС
        return "onWriteLog";
    }

}
