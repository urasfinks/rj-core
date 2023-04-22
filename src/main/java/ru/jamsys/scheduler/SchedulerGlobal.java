package ru.jamsys.scheduler;

public enum SchedulerGlobal {
    SCHEDULER_GLOBAL_STATISTIC_WRITE("SchedulerGlobalStatisticWrite", 1000),
    SCHEDULER_GLOBAL_STATISTIC_READ("SchedulerGlobalStatisticRead", 1000),
    ;

    private final String nameScheduler;
    private final long periodMillis;

    SchedulerGlobal(String nameScheduler, int periodMillis) {
        this.nameScheduler = nameScheduler;
        this.periodMillis = periodMillis;
    }

    public String getNameScheduler() {
        return nameScheduler;
    }

    public long getPeriodMillis() {
        return periodMillis;
    }

}
