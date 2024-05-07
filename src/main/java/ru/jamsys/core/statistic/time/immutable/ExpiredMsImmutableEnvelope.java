package ru.jamsys.core.statistic.time.immutable;

import lombok.Getter;

@Getter
public class ExpiredMsImmutableEnvelope<T> extends ExpiredMsImmutableImpl {

    final T value;

    public ExpiredMsImmutableEnvelope(T value, long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
        this.value = value;
    }

    public ExpiredMsImmutableEnvelope(T value, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
        this.value = value;
    }

}
