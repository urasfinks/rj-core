package ru.jamsys.task.handler;

import org.springframework.stereotype.Component;
import ru.jamsys.StatisticsCollector;
import ru.jamsys.component.Broker;
import ru.jamsys.component.Dictionary;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.task.instance.FlushStatisticCollectorTask;
import ru.jamsys.util.Util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@SuppressWarnings("unused")
public class FlushStatisticCollectorTaskHandler implements TaskHandler<FlushStatisticCollectorTask> {

    final Broker broker;

    final Dictionary dictionary;

    final ExceptionHandler exceptionHandler;

    String ip = Util.getIp();

    public FlushStatisticCollectorTaskHandler(Dictionary dictionary, Broker broker, ExceptionHandler exceptionHandler) {
        this.dictionary = dictionary;
        this.broker = broker;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void run(FlushStatisticCollectorTask task, AtomicBoolean isRun) throws Exception {
        StatisticSec statisticSec = new StatisticSec();
        Util.riskModifierCollection(isRun, dictionary.getListStatisticsCollector(), new StatisticsCollector[0], (StatisticsCollector statisticsCollector) -> {
            Map<String, String> parentTags = new LinkedHashMap<>();
            parentTags.put("measurement", statisticsCollector.getClass().getSimpleName());
            parentTags.put("host", ip);
            List<Statistic> statistics = statisticsCollector.flushAndGetStatistic(
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
                broker.add(StatisticSec.class, statisticSec);
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
