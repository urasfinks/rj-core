package ru.jamsys.core.component.item;

import lombok.Getter;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.template.cron.Cron;

@Getter
public class CronPromise {
    Cron cron;
    PromiseGenerator promiseGenerator;

    public CronPromise(Cron cron, PromiseGenerator promiseGenerator) {
        this.cron = cron;
        this.promiseGenerator = promiseGenerator;
    }
}