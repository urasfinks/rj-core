package ru.jamsys.core.extension.broker.persist;

import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.broker.Broker;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

public abstract class AbstractBrokerPersist<T extends ByteSerialization>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements Broker, ManagerElement {

    abstract public void commit(BrokerPersistElement<T> element);

}
