package ru.jamsys.core.extension.broker.persist;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.component.manager.item.BrokerMemoryImpl;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.function.Consumer;

public class BrokerPersistImpl<T extends ByteSerialization>
        extends BrokerMemoryImpl<T>
        implements Broker<T>, BrokerPersist<T> {

    public BrokerPersistImpl(
            String key,
            ApplicationContext applicationContext,
            Class<T> classItem,
            Consumer<T> onDrop
    ) {
        super(key, applicationContext, classItem, onDrop);
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
        DisposableExpirationMsImmutableEnvelope<T> add = super.add(envelope);
        return add;
    }
}
