package ru.jamsys.core.flat.template.cron.release;

public interface Cron3s extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "*/3";
    }

}
