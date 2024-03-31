package ru.jamsys.template.cron;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.thread.task.AbstractTask;

@Getter
@ToString
public class CronTask {
    private final Cron cron;
    private final AbstractTask task;

    public CronTask(Cron cron, AbstractTask task) {
        this.cron = cron;
        this.task = task;
    }
}
