package ru.jamsys.core.extension.expiration;


import ru.jamsys.core.flat.util.UtilDate;

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
        return "TimeOutException lastActivity: " + UtilDate.millis(timeStart).toDate().getDate()
                + "; timeOut: " + timeOut
                + "; now: " + UtilDate.millis(now).toDate().getDate();
    }

}
