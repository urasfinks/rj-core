package ru.jamsys.core.component.cron;

import lombok.Getter;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.promise.PromiseGenerator;

@Getter
public class CronPromise {
    Cron cron;
    PromiseGenerator promiseGenerator;

    public CronPromise(Cron cron, PromiseGenerator promiseGenerator) {
        this.cron = cron;
        this.promiseGenerator = promiseGenerator;
    }
}
