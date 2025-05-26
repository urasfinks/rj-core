package ru.jamsys.core.extension.expiration.immutable;

import lombok.Getter;
import lombok.Setter;

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
