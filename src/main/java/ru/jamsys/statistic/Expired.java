package ru.jamsys.statistic;

import ru.jamsys.util.Util;

public interface Expired {

    default boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    default boolean isExpired(long curTime) {
        return curTime > (getLastActivity() + getKeepAliveOnInactivityMs());
    }

    default void active() {
        setLastActivity(System.currentTimeMillis());
    }

    @SuppressWarnings("unused")
    default long getExpiryRemaining(long curTime) {
        return (getLastActivity() + getKeepAliveOnInactivityMs()) - curTime;
    }

    @SuppressWarnings("unused")
    default String getLastActivityFormat() {
        return Util.msToDataFormat(getLastActivity());
    }

    long getLastActivity();

    long getKeepAliveOnInactivityMs();

    void setLastActivity(long time);

    void setKeepAliveOnInactivityMs(long time);

}
