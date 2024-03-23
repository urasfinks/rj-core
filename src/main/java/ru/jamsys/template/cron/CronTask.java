package ru.jamsys.template.cron;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.thread.task.Task;

@Getter
@ToString
public class CronTask {
    private final Cron cron;
    private final Task task;

    public CronTask(Cron cron, Task task) {
        this.cron = cron;
        this.task = task;
    }
}
