package ru.jamsys.core.component.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ClassFinderComponent;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StatisticFlush implements Cron1s, PromiseGenerator {

    final ManagerElement<Broker<StatisticSec>, Void> brokerManagerElement;

    List<StatisticsFlushComponent> list = new ArrayList<>();

    String ip = Util.getIp();

    final ExceptionHandler exceptionHandler;

    public StatisticFlush(
            ClassFinderComponent classFinderComponent,
            ApplicationContext applicationContext,
            BrokerManager broker,
            ExceptionHandler exceptionHandler
    ) {
        brokerManagerElement = broker.get(ClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext), StatisticSec.class);
        this.exceptionHandler = exceptionHandler;
        classFinderComponent.findByInstance(StatisticsFlushComponent.class).forEach((Class<StatisticsFlushComponent> statisticsCollectorClass)
                -> list.add(applicationContext.getBean(statisticsCollectorClass)));

    }

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName(), 6_000L)
                .append(this.getClass().getName(), PromiseTaskExecuteType.IO, (AtomicBoolean isThreadRun) -> {
                    StatisticSec statisticSec = new StatisticSec();
                    UtilRisc.forEach(isThreadRun, list, (StatisticsFlushComponent statisticsFlushComponent) -> {
                        Map<String, String> parentTags = new LinkedHashMap<>();
                        String measurement = ClassNameImpl.getClassNameStatic(statisticsFlushComponent.getClass(), null);
                        parentTags.put("measurement", measurement);
                        parentTags.put("Host", ip);
                        List<Statistic> statistics = statisticsFlushComponent.flushAndGetStatistic(
                                parentTags,
                                null,
                                isThreadRun
                        );
                        if (statistics != null && !statistics.isEmpty()) {
                            statisticSec.getList().addAll(statistics);
                        }
                    });
                    if (!statisticSec.getList().isEmpty()) {
                        try {
                            brokerManagerElement.get().add(new ExpirationMsImmutableEnvelope<>(statisticSec, 6_000));
                        } catch (Exception e) {
                            exceptionHandler.handler(e);
                        }
                    }
                });
    }
}