package ru.jamsys.core.statistic.timer.ms;

import lombok.Getter;

@Getter
public class TimerMsEnvelope<T> extends TimerMsImpl {

    final T value;

    public TimerMsEnvelope(T value) {
        this.value = value;
    }

}
