package ru.jamsys.statistic;

import lombok.Setter;
import ru.jamsys.util.Util;

public abstract class AbstractExpired implements Expired {

    @Setter
    private long keepAliveOnInactivityMs = 60_000; // Время жизни очереди, если в ней нет активности

    private volatile long lastActivity = 0;

    public boolean isExpired() {
        return System.currentTimeMillis() > lastActivity + keepAliveOnInactivityMs;
    }

    @Override
    public boolean isExpired(long curTime) {
        return curTime > lastActivity + keepAliveOnInactivityMs;
    }

    @Override
    public String getLastActiveFormat() {
        return Util.msToDataFormat(lastActivity);
    }

    @Override
    public void active() {
        lastActivity = System.currentTimeMillis();
    }


}
