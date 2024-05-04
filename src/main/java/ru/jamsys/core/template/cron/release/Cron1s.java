package ru.jamsys.core.template.cron.release;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Cron1s extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "*";
    }

}
