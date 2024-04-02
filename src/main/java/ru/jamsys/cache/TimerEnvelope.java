package ru.jamsys.cache;

import lombok.Getter;
import ru.jamsys.statistic.TimeControllerImpl;

public class TimerEnvelope<T> extends TimeControllerImpl {

    @Getter
    final T value;

    public TimerEnvelope(T value) {
        this.value = value;
    }

}
