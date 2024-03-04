package ru.jamsys.task.handler;

import ru.jamsys.App;
import ru.jamsys.component.Broker;
import ru.jamsys.component.Component;
import ru.jamsys.component.Dictionary;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.Task;
import ru.jamsys.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@org.springframework.stereotype.Component
public class StatisticCollectorHandler extends AbstractHandler {

    @Override
    public void run(Task task, AtomicBoolean isRun) throws Exception {
        Dictionary dictionary = App.context.getBean(Dictionary.class);
        StatisticSec statisticSec = new StatisticSec();
        Util.riskModifierMap(
                isRun,
                dictionary.getMap(),
                Dictionary.getEmptyType(),
                (Class<? extends Component> k, Component v) -> {
                    if (v instanceof StatisticsCollector) {
                        Map<String, String> parentTags = new HashMap<>();
                        parentTags.put("measurement", k.getSimpleName());
                        parentTags.put("host", App.ip);
                        List<Statistic> statistics = ((StatisticsCollector) v).flushAndGetStatistic(
                                parentTags,
                                null,
                                isRun
                        );
                        if (statistics != null) {
                            statisticSec.getList().addAll(statistics);
                        }
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
