package ru.jamsys.core.statistic.time.immutable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Getter
public class ExpiredMsImmutableImpl implements ExpiredMsImmutable {

    private final long keepAliveOnInactivityMs; // Время жизни если нет активности

    private final long lastActivityMs;

    @Setter
    private Long stopTimeMs = null;

    public ExpiredMsImmutableImpl(long keepAliveOnInactivityMs, long lastActivityMs) {
        this.keepAliveOnInactivityMs = keepAliveOnInactivityMs;
        this.lastActivityMs = lastActivityMs;
    }

    public ExpiredMsImmutableImpl(long keepAliveOnInactivityMs) {
        this.keepAliveOnInactivityMs = keepAliveOnInactivityMs;
        this.lastActivityMs = System.currentTimeMillis();
    }

}
