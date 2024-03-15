package ru.jamsys.thread.handler;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.thread.task.ReadStatisticSecTask;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class ReadStatisticSecToConsoleHandler implements Handler<ReadStatisticSecTask> {

    final Broker broker;

    public ReadStatisticSecToConsoleHandler(Broker broker) {
        this.broker = broker;
    }

    @Override
    public void run(ReadStatisticSecTask task, AtomicBoolean isRun) throws Exception {
        Queue<StatisticSec> queue = broker.get(StatisticSec.class.getSimpleName());
        while (!queue.isEmpty() && isRun.get()) {
            StatisticSec statisticSec = queue.pollFirst();
            //Util.logConsole(UtilJson.toStringPretty(statisticSec, "{}"));
        }
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
