package ru.jamsys.core.flat.template.cron.release;

import ru.jamsys.core.flat.template.cron.Cron;

public interface CronConfigurator {

    String getCronTemplate();

    default boolean isTimeHasCome(Cron.CompileResult compileResult) {
        return true;
    }

}
