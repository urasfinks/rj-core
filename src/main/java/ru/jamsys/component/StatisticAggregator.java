package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.scheduler.SchedulerThreadFinal;
import ru.jamsys.statistic.AggregatorDataStatistic;
import ru.jamsys.statistic.Statistic;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class StatisticAggregator extends AbstractCoreComponent {

    private final Broker broker;
    private final ConcurrentLinkedQueue<Statistic> queue = new ConcurrentLinkedQueue<>();
    private final Scheduler scheduler;

    public StatisticAggregator(Scheduler scheduler, Broker broker) {
        this.scheduler = scheduler;
        this.broker = broker;
        SchedulerThreadFinal schedulerThread = (SchedulerThreadFinal) scheduler.get(SchedulerType.SCHEDULER_STATISTIC_WRITE);
        schedulerThread.setFinalProcedure(this::flushStatistic);
    }

    public void add(Statistic o) {
        if (o != null) {
            queue.add(o);
        }
    }

    @Override
    public void flushStatistic() {
        AggregatorDataStatistic<Statistic> aggregatorDataStatistic = new AggregatorDataStatistic<>();
        while (true) {
            Statistic poll = queue.poll();
            if (poll != null) {
                aggregatorDataStatistic.getList().add(poll);
            } else {
                break;
            }
        }
        try {
            broker.add(AggregatorDataStatistic.class, aggregatorDataStatistic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_WRITE).remove(this::flushStatistic);
    }
}
