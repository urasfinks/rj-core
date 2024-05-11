package ru.jamsys.core.statistic.expiration;

import ru.jamsys.core.util.Util;

public interface ExpirationMs {

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
    default long getInactivityTimeMs(long curTime) {
        if (isStop()) {
            return getStopTimeMs() - getLastActivityMs();
        } else {
            return curTime - getLastActivityMs();
        }
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getInactivityTimeMs() {
        return getInactivityTimeMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности
    default String getLastActivityFormat() {
        return Util.msToDataFormat(getLastActivityMs());
    }

    // Возвратит время, когда объект будет просрочен
    default String getExpiredFormat() {
        return Util.msToDataFormat(getLastActivityMs() + getKeepAliveOnInactivityMs());
    }

    // Зафиксировать конец активности
    default void stop(long curTime) {
        setStopTimeMs(curTime);
    }

    // Зафиксировать конец активности
    default void stop() {
        stop(System.currentTimeMillis());
    }

    // Зафиксировать конец активности
    default boolean isStop() {
        return getStopTimeMs() != null;
    }

    long getLastActivityMs();

    long getKeepAliveOnInactivityMs();

    void setStopTimeMs(Long timeMs);

    Long getStopTimeMs();

}
