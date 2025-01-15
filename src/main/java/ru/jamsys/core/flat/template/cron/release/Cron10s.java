package ru.jamsys.core.flat.template.cron.release;

@SuppressWarnings("unused")
public interface Cron10s extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "*/10";
    }

}
