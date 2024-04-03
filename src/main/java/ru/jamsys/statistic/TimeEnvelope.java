package ru.jamsys.statistic;

import lombok.Getter;

public class TimeEnvelope<T> extends TimeControllerImpl {

    @Getter
    final T value;

    public TimeEnvelope(T value) {
        this.value = value;
    }

}
