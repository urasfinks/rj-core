package ru.jamsys.core.pool;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

// Сколько прошло времени с момента события
public class EventTiming {

    private final AtomicBoolean happened = new AtomicBoolean(false);

    private final AtomicLong timeHappened = new AtomicLong(0);

    // Зафиксировать момент события
    public void event() {
        if (happened.compareAndSet(false, true)) {
            timeHappened.set(System.currentTimeMillis());
        }
    }

    // Сбросить событие
    public void reset() {
        happened.set(false);
        timeHappened.set(0);
    }

    // Сколько времени прошло с момента события, если оно было
    public long eventTimePassed() {
        if (happened.get()) {
            return System.currentTimeMillis() - timeHappened.get();
        }
        return 0;
    }

    // Было ли событие
    public boolean isHappened() {
        return happened.get();
    }

}
