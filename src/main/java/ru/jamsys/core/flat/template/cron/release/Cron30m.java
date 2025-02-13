package ru.jamsys.core.flat.template.cron.release;

@SuppressWarnings("unused")
public interface Cron30m extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "0 */30 * * *";
    }

}
