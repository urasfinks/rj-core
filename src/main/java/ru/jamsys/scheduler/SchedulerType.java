package ru.jamsys.scheduler;

import ru.jamsys.Util;

import java.util.function.Function;

public enum SchedulerType {
    SCHEDULER_STATISTIC_WRITE((name) -> new SchedulerThreadFinal(name, 1000)),
    SCHEDULER_STATISTIC_READ((name) -> new SchedulerThreadFinal(name, 1000)),
    STATISTIC_SYSTEM((name) -> new SchedulerThreadImpl(name, 1000));

    private final SchedulerThread schedulerThread;

    SchedulerType(Function<String, SchedulerThread> prc) {
        schedulerThread = prc.apply(getName());
    }

    public String getName() {
        return Util.snakeToCamel(name());
    }

    public SchedulerThread getThread() {
        return schedulerThread;
    }
}
