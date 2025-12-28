package ru.jamsys.core.flat.template.scheduler;

public interface SchedulerSequence {

    long next(long afterEpochMillis);

}
