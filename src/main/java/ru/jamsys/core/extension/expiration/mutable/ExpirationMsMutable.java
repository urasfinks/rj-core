package ru.jamsys.core.extension.expiration.mutable;


import ru.jamsys.core.extension.expiration.ExpirationMs;

public interface ExpirationMsMutable extends ExpirationMs {

    // Установить время последней активности
    default void markActive() {
        setLastActivityMs(System.currentTimeMillis());
    }

    void setInactivityTimeoutMs(long timeMs);

    default void setInactivityTimeoutSec(long timeSec) {
        setInactivityTimeoutMs(timeSec * 1_000);
    }

    default void setInactivityTimeoutMin(long timeMin) {
        setInactivityTimeoutMs(timeMin * 60_000);
    }

    void setLastActivityMs(long timeMs);

}
