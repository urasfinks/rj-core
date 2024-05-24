package ru.jamsys.core.flat.template.cron.release;

public interface Cron30s extends CronTemplate {

    @Override
    default String getCronTemplate() {
        return "*/30";
    }

}
