package ru.jamsys.core.extension.broker.memory;

import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractBrokerMemory<T>
        extends AbstractManagerElement
        implements
        AddToList<
                ExpirationMsImmutableEnvelope<T>,
                DisposableExpirationMsImmutableEnvelope<T> // Должны вернуть, что бы из вне можно было сделать remove
                > {

    abstract public ExpirationMsImmutableEnvelope<T> pollFirst();

    abstract public ExpirationMsImmutableEnvelope<T> pollLast();

    abstract public void remove(DisposableExpirationMsImmutableEnvelope<T> envelope);

    abstract public List<T> getCloneQueue(@Nullable AtomicBoolean run);

    abstract public List<T> getTailQueue(@Nullable AtomicBoolean run);

    // Добавление с явным указанием времени
    public DisposableExpirationMsImmutableEnvelope<T> add(T element, long curTime, long timeOut) {
        return add(new ExpirationMsImmutableEnvelope<>(element, timeOut, curTime));
    }

    public DisposableExpirationMsImmutableEnvelope<T> add(T element, long timeOutMs) {
        return add(new ExpirationMsImmutableEnvelope<>(element, timeOutMs));
    }

}
