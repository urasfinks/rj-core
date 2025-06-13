package ru.jamsys.core.extension.expiration.immutable;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ExpirationMsImmutableImpl implements ExpirationMsImmutable {

    private final long inactivityTimeoutMs; // Время жизни если нет активности

    private final long lastActivityMs;

    @Setter
    private Long stopTimeMs = null;

    public ExpirationMsImmutableImpl(long inactivityTimeoutMs, long lastActivityMs) {
        this.inactivityTimeoutMs = inactivityTimeoutMs;
        this.lastActivityMs = lastActivityMs;
    }

    public ExpirationMsImmutableImpl(long inactivityTimeoutMs) {
        this.inactivityTimeoutMs = inactivityTimeoutMs;
        this.lastActivityMs = System.currentTimeMillis();
    }

}
