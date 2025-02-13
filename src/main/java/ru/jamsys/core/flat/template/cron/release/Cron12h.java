package ru.jamsys.core.flat.template.cron.release;

@SuppressWarnings("unused")
public interface Cron12h extends CronConfigurator {

    @Override
    default String getCronTemplate() {
        return "0 0 */12 * *";
    }

}
