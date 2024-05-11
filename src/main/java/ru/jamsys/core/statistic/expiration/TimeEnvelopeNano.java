package ru.jamsys.core.statistic.expiration;

import lombok.Getter;

@Getter
public class TimeEnvelopeNano<T> extends TimeControllerNanoImpl {

    final T value;

    public TimeEnvelopeNano(T value) {
        this.value = value;
    }

}
