package ru.jamsys.statistic;

import lombok.Setter;
import ru.jamsys.util.Util;

public abstract class AbstractExpired implements Expired {

    @Setter
    private long keepAliveOnInactivityMs = 6_000; // Время жизни если нет активности

    private volatile long lastActivity = System.currentTimeMillis();

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    @Override
    public boolean isExpired(long curTime) {
        return curTime > (lastActivity + keepAliveOnInactivityMs);
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
