package ru.jamsys.template.cron;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.thread.generator.Generator;

@Getter
@ToString
public class CronTask {

    private final Cron cron;

    private final Generator generator;

    public CronTask(Cron cron, Generator generator) {
        this.cron = cron;
        this.generator = generator;
    }

}
