package ru.jamsys.core.statistic.time;


import ru.jamsys.core.util.Util;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface TimeControllerMs {

    default long getExpiredMs() {
        return getLastActivityMs() + getKeepAliveOnInactivityMs();
    }

    // Объект просрочен
    default boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    // Объект просрочен
    default boolean isExpired(long curTimeMs) {
        if (isStop()) {
            return false;
        }
        return curTimeMs > (getLastActivityMs() + getKeepAliveOnInactivityMs());
    }

    default TimeOutException getExpiredException() {
        return new TimeOutException(getLastActivityMs(), getKeepAliveOnInactivityMs());
    }

    // Установить время последней активности
    default void active() {
        setLastActivityMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    default long getExpiryRemainingMs(long curTime) {
        if (isStop()) {
            return 0L;
        }
        return (getLastActivityMs() + getKeepAliveOnInactivityMs()) - curTime;
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    default long getExpiryRemainingMs() {
        return getExpiryRemainingMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getOffsetLastActivityMs(long curTime) {
        if (isStop()) {
            return getTimeStopMs() - getLastActivityMs();
        } else {
            return curTime - getLastActivityMs();
        }
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getOffsetLastActivityMs() {
        return getOffsetLastActivityMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности
    default String getLastActivityFormat() {
        return Util.msToDataFormat(getLastActivityMs());
    }

    // Зафиксировать конец активности
    default void stop(long curTime) {
        setTimeStopMs(curTime);
    }

    // Зафиксировать конец активности
    default void stop() {
        stop(System.currentTimeMillis());
    }

    // Зафиксировать конец активности
    default boolean isStop() {
        return getTimeStopMs() != null;
    }

    default void setKeepAliveOnInactivitySec(long timeSec) {
        setKeepAliveOnInactivityMs(timeSec * 1_000);
    }

    default void setKeepAliveOnInactivityMin(long timeMin) {
        setKeepAliveOnInactivityMs(timeMin * 60_000);
    }

    void setLastActivityMs(long timeMs);

    long getLastActivityMs();

    void setKeepAliveOnInactivityMs(long timeMs);

    long getKeepAliveOnInactivityMs();

    void setTimeStopMs(Long timeMs);

    Long getTimeStopMs();

}
