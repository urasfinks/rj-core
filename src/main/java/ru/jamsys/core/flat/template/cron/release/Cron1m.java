package ru.jamsys.core.flat.template.cron.release;

public interface Cron1m extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "0";
    }

}
