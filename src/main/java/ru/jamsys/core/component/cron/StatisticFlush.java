package ru.jamsys.core.component.cron;

import com.influxdb.client.write.Point;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.broker.persist.BrokerPersist;
import ru.jamsys.core.extension.broker.persist.element.StatisticElement;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.extension.victoria.metrics.VictoriaMetricsConvert;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Component
@Lazy
@Getter
public class StatisticFlush extends PromiseGenerator implements Cron1s {

    List<StatisticsFlushComponent> list = new ArrayList<>();

    String host = Util.getHostname();

    private final ServicePromise servicePromise;

    private final ManagerConfiguration<BrokerPersist<StatisticElement>> brokerPersistManagerConfiguration;

    public StatisticFlush(
            ServiceClassFinder serviceClassFinder,
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        serviceClassFinder.findByInstance(StatisticsFlushComponent.class).forEach(statisticsCollectorClass
                -> list.add(App.get(statisticsCollectorClass)));
        brokerPersistManagerConfiguration = ManagerConfiguration.getInstance(
                BrokerPersist.class,
                java.util.UUID.randomUUID().toString(),
                "statistic",
                managerElement -> managerElement
                        .setup((bytes) -> new StatisticElement(new String(bytes)))
        );
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
                .append("main", (threadRun, _, _) -> {
                    StringBuilder sb = new StringBuilder();
                    for (StatisticsFlushComponent statisticsFlushComponent : list) {
                        for (StatisticDataHeader statisticDataHeader : statisticsFlushComponent.flushAndGetStatistic(threadRun)) {
                            Point influxPoint = VictoriaMetricsConvert.getInfluxFormat(statisticDataHeader);
                            influxPoint.addTag("host", host);
                            sb.append(influxPoint.toLineProtocol()).append("\n");
                        }
                    }
                    brokerPersistManagerConfiguration.get().add(new StatisticElement(sb.toString()));
                });
    }

}
