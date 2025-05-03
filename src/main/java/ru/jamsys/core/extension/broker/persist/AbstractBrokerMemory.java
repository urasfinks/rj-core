package ru.jamsys.core.extension.broker.persist;

import org.springframework.lang.Nullable;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractBrokerMemory<T>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements Broker<T>, ManagerElement {

    abstract public ExpirationMsImmutableEnvelope<T> pollFirst();

    abstract public ExpirationMsImmutableEnvelope<T> pollLast();

    abstract public void remove(DisposableExpirationMsImmutableEnvelope<T> envelope);

    abstract public List<T> getCloneQueue(@Nullable AtomicBoolean run);

    abstract public List<T> getTailQueue(@Nullable AtomicBoolean run);

}
