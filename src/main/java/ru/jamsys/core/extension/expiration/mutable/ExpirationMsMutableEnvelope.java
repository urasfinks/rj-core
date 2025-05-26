package ru.jamsys.core.extension.expiration.mutable;

import lombok.Getter;

@Getter
public class ExpirationMsMutableEnvelope<T> extends ExpirationMsMutableImpl {

    final T value;

    public ExpirationMsMutableEnvelope(T value) {
        this.value = value;
    }

}
