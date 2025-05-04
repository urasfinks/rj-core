package ru.jamsys.core.extension.broker.persist;

import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.broker.Broker;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

public abstract class AbstractBrokerPersist<T extends ByteSerialization>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements Broker<T>, ManagerElement {

    // Данные персистентного брокера могут читать в один момент времени сразу несколько групп
    // Сообщение реально вставляется одно, но его прочитать могут сразу несколько групп
    // по умолчанию

    abstract public ExpirationMsImmutableEnvelope<T> pollFirst(String groupRead);

    abstract public ExpirationMsImmutableEnvelope<T> pollLast(String groupRead);

    abstract public void commit(T element, String groupRead);

}
