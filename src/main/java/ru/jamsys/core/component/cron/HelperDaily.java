package ru.jamsys.core.component.cron;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.ServiceTimer;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.CronConfigurator;
import ru.jamsys.core.jt.Logger;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.resource.jdbc.SqlArgumentBuilder;

@SuppressWarnings("unused")
@Component
public class HelperDaily extends PromiseGenerator implements CronConfigurator {

    private final ServicePromise servicePromise;

    public HelperDaily() {
        this.servicePromise = App.get(ServicePromise.class);
        ServiceTimer serviceTimer = App.get(ServiceTimer.class);
    }

    @Override
    public boolean isTimeHasCome(Cron.CompileResult compileResult) {
        //return true;
        return compileResult.getBeforeTimestamp() != 0;
    }

    @Override
    public String getCronTemplate() {
        return "0 0 12 * *"; //  В 12:00:00 каждого любого дня месяца/года
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 60_000L)
                .thenWithResource(
                        "dropPartition",
                        JdbcResource.class,
                        "postgresql.remote.log",
                        (_, task, _, jdbcResource) -> jdbcResource.execute(
                                Logger.DROP_OLD_PARTITION,
                                new SqlArgumentBuilder()
                                        .add("days_threshold", App.get(ServiceProperty.class)
                                                .getOrThrow(
                                                        getCascadeKey("drop.old.partition.day.threshold"),
                                                        this
                                                )
                                                .get(Integer.class))

                        ))
                .thenWithResource(
                        "createPartition",
                        JdbcResource.class,
                        "postgresql.remote.log",
                        (_, task, _, jdbcResource) -> jdbcResource.execute(
                                Logger.CREATE_PARTITIONS,
                                new SqlArgumentBuilder()
                                        .add("days", App.get(ServiceProperty.class)
                                                .getOrThrow(
                                                        getCascadeKey("create.partitions.day"),
                                                        this
                                                )
                                                .get(Integer.class))

                        ))
                ;

    }

}
