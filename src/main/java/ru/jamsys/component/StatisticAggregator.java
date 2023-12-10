package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.scheduler.SchedulerCustom;
import ru.jamsys.scheduler.SchedulerGlobal;
import ru.jamsys.statistic.StatisticAggregatorData;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class StatisticAggregator extends AbstractCoreComponent {

    private final Broker broker;
    private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();
    private final Scheduler scheduler;

    public StatisticAggregator(Scheduler scheduler, Broker broker) {
        this.scheduler = scheduler;
        this.broker = broker;
        SchedulerCustom statWrite = scheduler.add(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_WRITE, null);
        statWrite.setLastProcedure(this::flushStatistic);
    }

    public void add(Object o) {
        if (o != null) {
            queue.add(o);
        }
    }

    @Override
    public void flushStatistic() {
        StatisticAggregatorData statisticAggregatorData = new StatisticAggregatorData();
        while (true) {
            Object poll = queue.poll();
            if (poll != null) {
                statisticAggregatorData.getList().add(poll);
            } else {
                break;
            }
        }
        try {
            broker.add(StatisticAggregatorData.class, statisticAggregatorData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.remove(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_WRITE, this::flushStatistic);
    }
}
