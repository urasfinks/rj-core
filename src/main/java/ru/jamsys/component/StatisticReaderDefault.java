package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
import ru.jamsys.UtilJson;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.statistic.AggregatorDataStatistic;
import ru.jamsys.statistic.Statistic;

@Component
@Lazy
public class StatisticReaderDefault extends AbstractCoreComponent {

    private final Scheduler scheduler;
    private final Broker broker;

    public StatisticReaderDefault(Scheduler scheduler, Broker broker) {
        this.scheduler = scheduler;
        this.broker = broker;
    }

    public void run(){
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_READ).add(this::flushStatistic);
    }

    @Override
    public void flushStatistic() {
        while (true) {
            AggregatorDataStatistic first = broker.pollFirst(AggregatorDataStatistic.class);
            if (first != null) {
                Util.logConsole(Thread.currentThread(), UtilJson.toString(first, null));
            } else {
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_READ).remove(this::flushStatistic);
    }
}
