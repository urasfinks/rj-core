package ru.jamsys.task.handler;

import ru.jamsys.App;
import ru.jamsys.component.Broker;
import ru.jamsys.component.Dictionary;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.AbstractTaskHandler;
import ru.jamsys.task.Task;
import ru.jamsys.util.Util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlushStatistic extends AbstractTaskHandler {

    String ip = Util.getIp();

    @Override
    public void run(Task task, AtomicBoolean isRun) throws Exception {
        Dictionary dictionary = App.context.getBean(Dictionary.class);
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
            App.context.getBean(Broker.class).add(StatisticSec.class, statisticSec);
        }
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }

}
