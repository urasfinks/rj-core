package ru.jamsys.core.statistic.time.immutable;

import lombok.Getter;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;

@Getter
public class ExpiredMsImmutableEnvelope<T> extends ExpiredMsMutableImpl {

    final T value;

    public ExpiredMsImmutableEnvelope(T value) {
        this.value = value;
    }

}
