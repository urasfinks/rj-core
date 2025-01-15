package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.CronConfigurator;
import ru.jamsys.core.promise.PromiseGenerator;

@Getter
public class CronTask {

    private final Cron cron;

    private final PromiseGenerator promiseGenerator;

    private final CronConfigurator cronConfigurator;

    @Setter
    private String index;

    public CronTask(Cron cron, PromiseGenerator promiseGenerator, CronConfigurator cronConfigurator) {
        this.cron = cron;
        this.promiseGenerator = promiseGenerator;
        this.cronConfigurator = cronConfigurator;
    }

}
