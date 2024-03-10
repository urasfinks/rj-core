package ru.jamsys.template.cron;

import lombok.Getter;
import ru.jamsys.task.Task;

@Getter
public class CronTask {
    private final Cron cron;
    private final Task task;

    public CronTask(Cron cron, Task task) {
        this.cron = cron;
        this.task = task;
    }
}
