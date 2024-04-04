package ru.jamsys.statistic;

import ru.jamsys.util.Util;

public interface TimeController {

    default long getExpiredMs() {
        return getLastActivity() + getKeepAliveOnInactivityMs();
    }

    // Объект просрочен
    default boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    // Объект просрочен
    default boolean isExpired(long curTime) {
        if (isStop()) {
            return false;
        }
        return curTime > (getLastActivity() + getKeepAliveOnInactivityMs());
    }

    // Установить время последней активности
    default void active() {
        setLastActivity(System.currentTimeMillis());
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    @SuppressWarnings("unused")
    default long getExpiryRemainingMs(long curTime) {
        if (isStop()) {
            return 0L;
        }
        return (getLastActivity() + getKeepAliveOnInactivityMs()) - curTime;
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    @SuppressWarnings("unused")
    default long getExpiryRemainingMs() {
        return getExpiryRemainingMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    @SuppressWarnings("unused")
    default long getOffsetLastActivityMs(long curTime) {
        if (isStop()) {
            return getTimeStop() - getLastActivity();
        } else {
            return curTime - getLastActivity();
        }
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    @SuppressWarnings("unused")
    default long getOffsetLastActivityMs() {
        return getOffsetLastActivityMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности
    @SuppressWarnings("unused")
    default String getLastActivityFormat() {
        return Util.msToDataFormat(getLastActivity());
    }

    // Зафиксировать конец активности
    @SuppressWarnings("unused")
    default void stop(long curTime) {
        setTimeStop(curTime);
    }

    // Зафиксировать конец активности
    @SuppressWarnings("unused")
    default void stop() {
        stop(System.currentTimeMillis());
    }

    // Зафиксировать конец активности
    @SuppressWarnings("unused")
    default boolean isStop() {
        return getTimeStop() != null;
    }

    //######### Обязательная реализация при имплементации##########

    void setLastActivity(long time);

    long getLastActivity();

    void setKeepAliveOnInactivityMs(long time);

    long getKeepAliveOnInactivityMs();

    void setTimeStop(Long time);

    Long getTimeStop();

}
