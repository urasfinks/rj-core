package ru.jamsys.core.component.cron;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
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

@Component
@Lazy
public class StatisticFlush extends RepositoryPropertiesField implements Cron1s, PromiseGenerator, UniqueClassName {

    final Broker<StatisticSec> broker;

    List<StatisticsFlushComponent> list = new ArrayList<>();

    String ip = Util.getIp();

    final ExceptionHandler exceptionHandler;

    private final ServicePromise servicePromise;

    @Getter
    @PropertyName("run.args.remote.statistic")
    private Boolean remoteStatistic = true;

    public StatisticFlush(
            ServiceClassFinder serviceClassFinder,
            ApplicationContext applicationContext,
            ManagerBroker broker,
            ExceptionHandler exceptionHandler,
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        this.broker = broker.get(
                UniqueClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext),
                StatisticSec.class
        );
        this.exceptionHandler = exceptionHandler;
        serviceClassFinder.findByInstance(StatisticsFlushComponent.class).forEach(statisticsCollectorClass
                -> list.add(applicationContext.getBean(statisticsCollectorClass)));

        serviceProperty.getFactory().getPropertiesAgent(
                null,
                this,
                null,
                false
        );
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 6_000L)
                .append("main", (threadRun, _, _) -> {
                    StatisticSec statisticSec = new StatisticSec();
                    UtilRisc.forEach(threadRun, list, (StatisticsFlushComponent statisticsFlushComponent) -> {
                        Map<String, String> parentTags = new LinkedHashMap<>();
                        String measurement = UniqueClassNameImpl.getClassNameStatic(statisticsFlushComponent.getClass(), null);
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
                    if (!statisticSec.getList().isEmpty() && remoteStatistic) {
                        broker.add(new ExpirationMsImmutableEnvelope<>(statisticSec, 6_000));
                    }
                });
    }
}
