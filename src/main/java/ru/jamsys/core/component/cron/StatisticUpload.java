package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.broker.persist.BrokerPersist;
import ru.jamsys.core.extension.broker.persist.X;
import ru.jamsys.core.extension.broker.persist.element.StatisticElement;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyBuilder;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.plugin.http.resource.influx.InfluxPlugin;
import ru.jamsys.core.plugin.http.resource.influx.InfluxRepositoryProperty;
import ru.jamsys.core.plugin.http.resource.victoria.metrics.VictoriaMetricsRepositoryProperty;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Component
@Lazy
public class StatisticUpload extends PromiseGenerator implements Cron5s {

    private final ServicePromise servicePromise;

    private final StatisticFlush statisticFlush;

    private final StatisticUploadRepositoryProperty property = new StatisticUploadRepositoryProperty();

    public StatisticUpload(ServicePromise servicePromise, StatisticFlush statisticFlush) {
        this.servicePromise = servicePromise;
        this.statisticFlush = statisticFlush;
    }

    @Override
    public boolean isTimeHasCome(Cron.CompileResult compileResult) {
        // Запускать будем спустя секунду, а не прямо в момент старта
        // Это влияет на тесты
        return compileResult.getBeforeTimestamp() != 0;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 6_000L)
                .appendWithResource(
                        "main",
                        HttpResource.class,
                        "influx",
                        (threadRun, promiseTask, promise, resource) -> {
                            BrokerPersist<StatisticElement> statisticElementBrokerPersist =
                                    statisticFlush.getBrokerPersistManagerConfiguration().get();
                            List<X<StatisticElement>> list = new ArrayList<>();
                            StringBuilder sb = new StringBuilder();
                            while (sb.length() < property.getBatchMaxSizeByte()) {
                                X<StatisticElement> poll = statisticElementBrokerPersist.poll();
                                if (poll == null) {
                                    break;
                                }
                                list.add(poll);
                                sb.append(poll.getElement().getValue());
                            }
                            HttpResponse execute = InfluxPlugin.execute(
                                    resource.prepare(),
                                    new RepositoryPropertyBuilder<>(new InfluxRepositoryProperty(), resource.getNs())
                                            .applyServiceProperty()
                                            .apply(VictoriaMetricsRepositoryProperty.Fields.bodyRaw, sb.toString())
                                            .build()
                            );
                            if (execute.isSuccess()) {
                                list.forEach(statisticElementBrokerPersist::commit);
                            } else {
                                UtilLog.printError(execute);
                            }
                        })
                ;
    }

}
