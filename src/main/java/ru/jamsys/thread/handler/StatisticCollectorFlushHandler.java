package ru.jamsys.thread.handler;

import org.springframework.stereotype.Component;
import ru.jamsys.component.Broker;
import ru.jamsys.component.Dictionary;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.thread.task.StatisticCollectorFlush;
import ru.jamsys.util.Util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@SuppressWarnings("unused")
public class StatisticCollectorFlushHandler implements Handler<StatisticCollectorFlush> {

    final Broker broker;

    final Dictionary dictionary;

    final ExceptionHandler exceptionHandler;

    String ip = Util.getIp();

    public StatisticCollectorFlushHandler(Dictionary dictionary, Broker broker, ExceptionHandler exceptionHandler) {
        this.dictionary = dictionary;
        this.broker = broker;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void run(StatisticCollectorFlush task, AtomicBoolean isRun) throws Exception {
        StatisticSec statisticSec = new StatisticSec();
        Util.riskModifierCollection(isRun, dictionary.getListStatisticsCollectorComponent(), new StatisticsCollectorComponent[0], (StatisticsCollectorComponent statisticsCollectorComponent) -> {
            Map<String, String> parentTags = new LinkedHashMap<>();
            parentTags.put("measurement", statisticsCollectorComponent.getClass().getSimpleName());
            parentTags.put("host", ip);
            List<Statistic> statistics = statisticsCollectorComponent.flushAndGetStatistic(
                    parentTags,
                    null,
                    isRun
            );
            if (statistics != null && !statistics.isEmpty()) {
                statisticSec.getList().addAll(statistics);
            }
        });
        if (!statisticSec.getList().isEmpty()) {
            try {
                broker.add(StatisticSec.class.getSimpleName(), statisticSec);
            } catch (Exception e) {
                exceptionHandler.handler(e);
            }
        }
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
