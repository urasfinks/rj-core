package ru.jamsys.core.extension.raw.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

@Getter
@Setter
public class Transaction<T> {
    private DisposableExpirationMsImmutableEnvelope<T> id;
    private T object;

    public Transaction(DisposableExpirationMsImmutableEnvelope<T> id, T object) {
        this.id = id;
        this.object = object;
    }
}
