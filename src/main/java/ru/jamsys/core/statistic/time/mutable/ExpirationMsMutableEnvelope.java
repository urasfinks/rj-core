package ru.jamsys.core.statistic.time.mutable;

import lombok.Getter;

@Getter
public class ExpirationMsMutableEnvelope<T> extends ExpirationMsMutableImpl {

    final T value;

    public ExpirationMsMutableEnvelope(T value) {
        this.value = value;
    }

}
