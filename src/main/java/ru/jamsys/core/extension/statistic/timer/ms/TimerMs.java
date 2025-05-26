package ru.jamsys.core.extension.statistic.timer.ms;


public interface TimerMs {

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
        if (isStop()) {
            return getTimeStopMs() - getLastActivityMs();
        } else {
            return getOffsetLastActivityMs(System.currentTimeMillis());
        }
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

    long getLastActivityMs();

    void setTimeStopMs(Long timeMs);

    Long getTimeStopMs();

}
