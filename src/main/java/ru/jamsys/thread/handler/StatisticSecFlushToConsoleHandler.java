package ru.jamsys.thread.handler;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.extension.IgnoreClassFinder;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.thread.task.StatisticSecFlush;
import ru.jamsys.util.Util;
import ru.jamsys.util.UtilJson;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
@IgnoreClassFinder
public class StatisticSecFlushToConsoleHandler implements Handler<StatisticSecFlush> {

    final Broker broker;

    public StatisticSecFlushToConsoleHandler(Broker broker) {
        this.broker = broker;
    }

    @Override
    public void run(StatisticSecFlush task, AtomicBoolean isRun) throws Exception {
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
