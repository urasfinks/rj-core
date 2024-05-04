package ru.jamsys.core.statistic.time;

import ru.jamsys.core.util.Util;

public class TimeOutException extends Exception {

    final long timeStart;
    final long timeOut;
    final long now = System.currentTimeMillis();

    public TimeOutException(long timeStart, long timeOut) {
        this.timeStart = timeStart;
        this.timeOut = timeOut;
    }

    @Override
    public String getMessage() {
        return "TimeOutException lastActivity: " + Util.msToDataFormat(timeStart)
                + "; timeOut: " + timeOut
                + "; now: " + Util.msToDataFormat(now);
    }
}
