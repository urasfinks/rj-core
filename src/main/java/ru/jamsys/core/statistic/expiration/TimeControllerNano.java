package ru.jamsys.core.statistic.expiration;


@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface TimeControllerNano {

    // Установить время последней активности
    default void active() {
        setLastActivityNano(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getOffsetLastActivityNano(long curTime) {
        if (isStop()) {
            return getTimeStopNano() - getLastActivityNano();
        } else {
            return curTime - getLastActivityNano();
        }
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getOffsetLastActivityNano() {
        if (isStop()) {
            return getTimeStopNano() - getLastActivityNano();
        } else {
            return getOffsetLastActivityNano(System.nanoTime());
        }
    }

    // Зафиксировать конец активности
    default void stop(long curTime) {
        setTimeStopNano(curTime);
    }

    // Зафиксировать конец активности
    default void stop() {
        stop(System.nanoTime());
    }

    // Зафиксировать конец активности
    default boolean isStop() {
        return getTimeStopNano() != null;
    }

    void setLastActivityNano(long timeNano);

    long getLastActivityNano();

    void setTimeStopNano(Long timeMs);

    Long getTimeStopNano();

}
