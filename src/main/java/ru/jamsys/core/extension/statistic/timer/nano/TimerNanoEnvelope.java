package ru.jamsys.core.extension.statistic.timer.nano;

import lombok.Getter;

@Getter
public class TimerNanoEnvelope<T> extends TimerNanoImpl {

    final T value;

    public TimerNanoEnvelope(T value) {
        this.value = value;
    }

}
