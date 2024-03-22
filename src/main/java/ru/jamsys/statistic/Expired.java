package ru.jamsys.statistic;

public interface Expired {

    @SuppressWarnings("unused")
    boolean isExpired();

    boolean isExpired(long curTime);

    String getLastActiveFormat();

    void active();

    void setKeepAliveOnInactivityMs(long ms);

    long getExpiryRemaining(long curTime);

}
