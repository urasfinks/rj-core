package ru.jamsys.core.flat.template.cron.release;

@SuppressWarnings("unused")
public interface Cron30s extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "*/30";
    }

}
