package ru.jamsys.core.flat.template.cron.release;

public interface Cron1s extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "*";
    }

}
