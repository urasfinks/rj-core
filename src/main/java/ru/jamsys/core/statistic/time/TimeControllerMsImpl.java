package ru.jamsys.core.statistic.time;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TimeControllerMsImpl implements TimeControllerMs {

    private long keepAliveOnInactivityMs = 6_000; // Время жизни если нет активности

    private volatile long lastActivityMs = System.currentTimeMillis();

    private Long timeStopMs = null;

}
