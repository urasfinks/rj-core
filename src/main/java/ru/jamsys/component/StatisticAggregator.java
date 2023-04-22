package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.scheduler.SchedulerCustom;
import ru.jamsys.scheduler.SchedulerGlobal;
import ru.jamsys.statistic.StatisticAggregatorData;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class StatisticAggregator extends AbstractCoreComponent {

    private Broker broker;
    private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();
    private final ConfigurableApplicationContext applicationContext;
    private final Scheduler scheduler;

    public StatisticAggregator(ConfigurableApplicationContext applicationContext, Scheduler scheduler) {
        this.applicationContext = applicationContext;
        this.scheduler = scheduler;
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
            Object peek = queue.poll();
            if (peek != null) {
                statisticAggregatorData.getList().add(peek);
            } else {
                break;
            }
        }
        if (broker == null) {
            broker = applicationContext.getBean(Broker.class);
        }
        broker.add(StatisticAggregatorData.class, statisticAggregatorData);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.remove(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_WRITE, this::flushStatistic);
    }
}
