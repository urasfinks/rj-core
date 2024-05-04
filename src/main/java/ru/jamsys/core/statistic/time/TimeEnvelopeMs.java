package ru.jamsys.core.statistic.time;

import lombok.Getter;

@Getter
public class TimeEnvelopeMs<T> extends TimeControllerMsImpl {

    final T value;

    public TimeEnvelopeMs(T value) {
        this.value = value;
    }

}
