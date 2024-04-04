package ru.jamsys.template.cron;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.task.AbstractTask;

@Getter
@ToString
public class CronTask {

    private final Cron cron;

    private final TimeEnvelope<AbstractTask> timeEnvelope;

    public CronTask(Cron cron, TimeEnvelope<AbstractTask> timeEnvelope) {
        this.cron = cron;
        this.timeEnvelope = timeEnvelope;
    }

}
