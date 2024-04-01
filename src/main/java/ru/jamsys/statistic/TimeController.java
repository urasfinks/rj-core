package ru.jamsys.statistic;

import ru.jamsys.util.Util;

public interface TimeController {

    // Объект закончившись
    default boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    // Объект закончившись
    default boolean isExpired(long curTime) {
        return curTime > (getLastActivity() + getKeepAliveOnInactivityMs());
    }

    // Установить время последней активности
    default void active() {
        setLastActivity(System.currentTimeMillis());
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    @SuppressWarnings("unused")
    default long getExpiryRemainingMs(long curTime) {
        return (getLastActivity() + getKeepAliveOnInactivityMs()) - curTime;
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    @SuppressWarnings("unused")
    default long getExpiryRemainingMs() {
        return (getLastActivity() + getKeepAliveOnInactivityMs()) - System.currentTimeMillis();
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    @SuppressWarnings("unused")
    default long getOffsetLastActivityMs(long curTime) {
        Long timeStop = getTimeStop();
        if (timeStop != null) {
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
