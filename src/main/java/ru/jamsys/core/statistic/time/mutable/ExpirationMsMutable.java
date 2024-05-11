package ru.jamsys.core.statistic.time.mutable;


import ru.jamsys.core.statistic.time.ExpirationMs;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface ExpirationMsMutable extends ExpirationMs {

    // Установить время последней активности
    default void active() {
        setLastActivityMs(System.currentTimeMillis());
    }

    void setKeepAliveOnInactivityMs(long timeMs);

    default void setKeepAliveOnInactivitySec(long timeSec) {
        setKeepAliveOnInactivityMs(timeSec * 1_000);
    }

    default void setKeepAliveOnInactivityMin(long timeMin) {
        setKeepAliveOnInactivityMs(timeMin * 60_000);
    }

    void setLastActivityMs(long timeMs);

}
