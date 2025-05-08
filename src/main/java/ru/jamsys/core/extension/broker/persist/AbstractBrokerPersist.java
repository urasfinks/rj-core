package ru.jamsys.core.extension.broker.persist;

import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.broker.Broker;

public abstract class AbstractBrokerPersist<T extends ByteSerialization>
        extends AbstractManagerElement
        implements Broker {

    abstract public void commit(BrokerPersistElement<T> element) throws Throwable;

}
