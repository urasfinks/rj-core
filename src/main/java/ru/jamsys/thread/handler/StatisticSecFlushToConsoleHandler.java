package ru.jamsys.thread.handler;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.component.item.BrokerQueue;
import ru.jamsys.component.Broker;
import ru.jamsys.extension.IgnoreClassFinder;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.StatisticSecFlush;
import ru.jamsys.util.Util;
import ru.jamsys.util.UtilJson;

@SuppressWarnings("unused")
@Component
@Lazy
@IgnoreClassFinder
public class StatisticSecFlushToConsoleHandler implements Handler<StatisticSecFlush> {

    final Broker<StatisticSec> broker;

    public StatisticSecFlushToConsoleHandler(Broker<StatisticSec> broker) {
        this.broker = broker;
    }

    @Override
    public void run(StatisticSecFlush task, ThreadEnvelope threadEnvelope) throws Exception {
        BrokerQueue<StatisticSec> queue = broker.getItem(StatisticSec.class.getSimpleName());
        while (!queue.isEmpty() && threadEnvelope.getIsWhile().get()) {
            TimeEnvelope<StatisticSec> statisticSec = queue.pollFirst();
            Util.logConsole(UtilJson.toStringPretty(statisticSec.getValue(), "{}"));
        }
    }

}
