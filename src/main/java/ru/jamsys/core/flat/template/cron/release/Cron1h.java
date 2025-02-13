package ru.jamsys.core.flat.template.cron.release;

@SuppressWarnings("unused")
public interface Cron1h extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "0 0 * * *";
    }

}
