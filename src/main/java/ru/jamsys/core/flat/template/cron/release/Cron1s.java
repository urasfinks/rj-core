package ru.jamsys.core.flat.template.cron.release;

public interface Cron1s extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "*";
    }

}
