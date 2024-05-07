package ru.jamsys.core.statistic.time.mutable;

import lombok.Getter;

@Getter
public class ExpiredMsMutableEnvelope<T> extends ExpiredMsMutableImpl {

    final T value;

    public ExpiredMsMutableEnvelope(T value) {
        this.value = value;
    }

}
