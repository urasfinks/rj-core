package ru.jamsys.core.flat.template.cron.release;

public interface Cron10s extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "*/10";
    }

}
