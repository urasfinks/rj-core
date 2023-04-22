package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
import ru.jamsys.scheduler.SchedulerGlobal;
import ru.jamsys.statistic.StatisticAggregatorData;

@Component
@Lazy
public class StatisticReaderDefault extends AbstractCoreComponent {

    private final Scheduler scheduler;
    private final Broker broker;

    public StatisticReaderDefault(Scheduler scheduler, Broker broker) {
        this.scheduler = scheduler;
        this.broker = broker;
        scheduler.add(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_READ, this::flushStatistic);
    }

    @Override
    public void flushStatistic() {
        while (true) {
            StatisticAggregatorData first = broker.pollFirst(StatisticAggregatorData.class);
            if (first != null) {
                Util.logConsole(Thread.currentThread(), Util.jsonObjectToString(first));
            } else {
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.remove(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_READ, this::flushStatistic);
    }
}
