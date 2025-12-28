package ru.jamsys.core.flat.template.scheduler;

public interface SchedulerTemplate {

    interface Builder<T extends SchedulerTemplate> {

        T buildTemplate();

        SchedulerSequence buildSequence();

    }

}
