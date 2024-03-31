package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractExpired implements Expired {

    @Getter
    @Setter
    private long keepAliveOnInactivityMs = 6_000; // Время жизни если нет активности

    @Getter
    @Setter
    private volatile long lastActivity = System.currentTimeMillis();

}
