package ru.jamsys.core.template.cron.release;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Cron3s extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "*/3";
    }

}