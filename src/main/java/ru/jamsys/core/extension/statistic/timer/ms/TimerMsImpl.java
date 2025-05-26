package ru.jamsys.core.extension.statistic.timer.ms;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimerMsImpl implements TimerMs {

    private long lastActivityMs = System.currentTimeMillis();

    private Long timeStopMs = null;

}
