package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.Core;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@FieldNameConstants
@Component
@Lazy
public class StatisticFlush extends AnnotationPropertyExtractor<Boolean> implements Cron1s, PromiseGenerator, CascadeKey {

    final BrokerMemory<StatisticSec> broker;

    List<StatisticsFlushComponent> list = new ArrayList<>();

    String ip = Util.getIp();

    private final ServicePromise servicePromise;

    @SuppressWarnings("all")
    @Getter
    @PropertyKey("remote")
    private Boolean remote = false;

    public StatisticFlush(
            ServiceClassFinder serviceClassFinder,
            ApplicationContext applicationContext,
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        this.broker = applicationContext.getBean(Core.class).getStatisticSecBroker();
        serviceClassFinder.findByInstance(StatisticsFlushComponent.class).forEach(statisticsCollectorClass
                -> list.add(applicationContext.getBean(statisticsCollectorClass)));
        new PropertyDispatcher<>(
                serviceProperty,
                null,
                this,
                "statistic.uploader"
        );
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 6_000L)
                .append("main", (threadRun, _, _) -> {
                    StatisticSec statisticSec = new StatisticSec();
                    UtilRisc.forEach(threadRun, list, (StatisticsFlushComponent statisticsFlushComponent) -> {
                        Map<String, String> parentTags = new LinkedHashMap<>();
                        String measurement = App.getUniqueClassName(statisticsFlushComponent.getClass());
                        parentTags.put("measurement", measurement);
                        parentTags.put("Host", ip);
                        List<Statistic> statistics = statisticsFlushComponent.flushAndGetStatistic(
                                parentTags,
                                null,
                                threadRun
                        );
                        if (statistics != null && !statistics.isEmpty()) {
                            statisticSec.getList().addAll(statistics);
                        }
                    });
                    // Несмотря на remoteStatistic надо с сервисов сбрасывать статистику
                    // Так что мы будем всё собирать, но отправлять не будем
                    if (!statisticSec.getList().isEmpty() && remote) {
                        broker.add(new ExpirationMsImmutableEnvelope<>(statisticSec, 6_000));
                    }
                });
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeKey getParentCascadeKey() {
        return App.cascadeName;
    }

}
