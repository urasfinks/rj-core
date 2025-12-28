package ru.jamsys.core.extension.expiration;

import ru.jamsys.core.flat.util.UtilDateOld;

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
        return "TimeOutException lastActivity: " + UtilDateOld.msFormat(timeStart)
                + "; timeOut: " + timeOut
                + "; now: " + UtilDateOld.msFormat(now);
    }

}
