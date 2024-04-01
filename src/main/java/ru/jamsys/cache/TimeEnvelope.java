package ru.jamsys.cache;

import lombok.Getter;
import ru.jamsys.statistic.TimeControllerImpl;

public class TimeEnvelope<T> extends TimeControllerImpl {

    @Getter
    final T value;

    public TimeEnvelope(T value) {
        this.value = value;
    }

}
