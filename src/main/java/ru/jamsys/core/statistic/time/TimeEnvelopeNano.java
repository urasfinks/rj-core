package ru.jamsys.core.statistic.time;

import lombok.Getter;

@Getter
public class TimeEnvelopeNano<T> extends TimeControllerNanoImpl {

    final T value;

    public TimeEnvelopeNano(T value) {
        this.value = value;
    }

}
