package ru.jamsys.task.handler;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.IgnoreClassFinder;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.task.instance.ReadStatisticSecTask;
import ru.jamsys.util.Util;
import ru.jamsys.util.UtilJson;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class ReadStatisticSecToConsoleTaskHandler implements TaskHandler<ReadStatisticSecTask> {

    final Broker broker;

    public ReadStatisticSecToConsoleTaskHandler(Broker broker) {
        this.broker = broker;
    }

    @Override
    public void run(ReadStatisticSecTask task, AtomicBoolean isRun) throws Exception {
        Queue<StatisticSec> queue = broker.get(StatisticSec.class.getSimpleName());
        while (!queue.isEmpty() && isRun.get()) {
            StatisticSec statisticSec = queue.pollFirst();
            Util.logConsole(UtilJson.toStringPretty(statisticSec, "{}"));
        }
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
