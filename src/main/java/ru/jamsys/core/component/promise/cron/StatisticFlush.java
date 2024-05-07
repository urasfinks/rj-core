package ru.jamsys.core.component.promise.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.api.BrokerManager;
import ru.jamsys.core.component.api.ClassFinder;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskType;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;
import ru.jamsys.core.template.cron.release.Cron1s;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class StatisticFlush implements Cron1s, PromiseGenerator {

    final BrokerManager<StatisticSec> broker;

    List<StatisticsFlushComponent> list = new ArrayList<>();

    String ip = Util.getIp();

    final String brokerIndex;

    final ExceptionHandler exceptionHandler;

    public StatisticFlush(
            ClassFinder classFinder,
            ApplicationContext applicationContext,
            BrokerManager<StatisticSec> broker,
            ExceptionHandler exceptionHandler
    ) {
        this.broker = broker;
        this.brokerIndex = ClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext);
        this.exceptionHandler = exceptionHandler;
        classFinder.findByInstance(StatisticsFlushComponent.class).forEach((Class<StatisticsFlushComponent> statisticsCollectorClass)
                -> list.add(applicationContext.getBean(statisticsCollectorClass)));
    }

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName())
                .append(this.getClass().getName(), PromiseTaskType.IO, (AtomicBoolean isThreadRun) -> {
                    StatisticSec statisticSec = new StatisticSec();
                    Util.riskModifierCollection(isThreadRun, list, new StatisticsFlushComponent[0], (StatisticsFlushComponent statisticsFlushComponent) -> {
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
                            broker.add(brokerIndex, new ExpiredMsMutableEnvelope<>(statisticSec));
                        } catch (Exception e) {
                            exceptionHandler.handler(e);
                        }
                    }
                });
    }
}
