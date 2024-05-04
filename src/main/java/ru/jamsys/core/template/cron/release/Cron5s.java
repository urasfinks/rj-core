package ru.jamsys.core.template.cron.release;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Cron5s extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "*/5";
    }

}
