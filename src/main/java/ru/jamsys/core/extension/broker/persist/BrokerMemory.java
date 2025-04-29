package ru.jamsys.core.extension.broker.persist;

import org.springframework.lang.Nullable;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface BrokerMemory<T> extends Broker<T>, ManagerElement {

    ExpirationMsImmutableEnvelope<T> pollFirst();

    ExpirationMsImmutableEnvelope<T> pollLast();

    void remove(DisposableExpirationMsImmutableEnvelope<T> envelope);

    List<T> getCloneQueue(@Nullable AtomicBoolean run);

    List<T> getTailQueue(@Nullable AtomicBoolean run);

}
