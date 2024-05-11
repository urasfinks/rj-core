package ru.jamsys.core.statistic.expiration.immutable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Getter
public class ExpirationMsImmutableImpl implements ExpirationMsImmutable {

    private final long keepAliveOnInactivityMs; // Время жизни если нет активности

    private final long lastActivityMs;

    @Setter
    private Long stopTimeMs = null;

    public ExpirationMsImmutableImpl(long keepAliveOnInactivityMs, long lastActivityMs) {
        this.keepAliveOnInactivityMs = keepAliveOnInactivityMs;
        this.lastActivityMs = lastActivityMs;
    }

    public ExpirationMsImmutableImpl(long keepAliveOnInactivityMs) {
        this.keepAliveOnInactivityMs = keepAliveOnInactivityMs;
        this.lastActivityMs = System.currentTimeMillis();
    }

}
