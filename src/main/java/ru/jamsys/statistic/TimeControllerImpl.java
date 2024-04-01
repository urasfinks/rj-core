package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;

public class TimeControllerImpl implements TimeController {

    @Getter
    @Setter
    private long keepAliveOnInactivityMs = 6_000; // Время жизни если нет активности

    @Getter
    @Setter
    private long lastActivity = System.currentTimeMillis();

    @Getter
    @Setter
    private Long timeStop = null;
}
