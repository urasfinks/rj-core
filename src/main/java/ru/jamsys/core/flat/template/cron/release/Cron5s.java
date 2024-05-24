package ru.jamsys.core.flat.template.cron.release;

public interface Cron5s extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "*/5";
    }

}
