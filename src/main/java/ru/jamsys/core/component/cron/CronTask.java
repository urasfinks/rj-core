package ru.jamsys.core.component.cron;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.CronConfigurator;
import ru.jamsys.core.promise.PromiseGenerator;

@SuppressWarnings("unused")
@Getter
public class CronTask {

    private final Cron cron;

    private final PromiseGenerator promiseGenerator;

    private final CronConfigurator cronConfigurator;

    public CronTask(Cron cron, PromiseGenerator promiseGenerator, CronConfigurator cronConfigurator) {
        this.cron = cron;
        this.promiseGenerator = promiseGenerator;
        this.cronConfigurator = cronConfigurator;
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("cronConfigurator", cronConfigurator)
                ;
    }

}
