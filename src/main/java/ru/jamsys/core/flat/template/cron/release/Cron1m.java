package ru.jamsys.core.flat.template.cron.release;

@SuppressWarnings("unused")
public interface Cron1m extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "0";
    }

}
