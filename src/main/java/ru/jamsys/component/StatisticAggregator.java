package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.scheduler.SchedulerThreadFinal;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.statistic.AggregatorDataStatistic;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.WrapStatistic;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class StatisticAggregator extends AbstractCoreComponent {

    private final Broker broker;
    private final ConcurrentLinkedQueue<WrapStatistic> queue = new ConcurrentLinkedQueue<>();
    private final Scheduler scheduler;

    public StatisticAggregator(Scheduler scheduler, Broker broker) {
        this.scheduler = scheduler;
        this.broker = broker;
    }

    public void run() {
        SchedulerThreadFinal schedulerThread = (SchedulerThreadFinal) scheduler.get(SchedulerType.SCHEDULER_STATISTIC_WRITE);
        schedulerThread.setFinalProcedure(this::flushStatistic);
    }

    public void add(Class<?> classOwner, Statistic statistic) {
        if (statistic != null) {
            queue.add(new WrapStatistic(classOwner, statistic));
        }
    }

    @Override
    public void flushStatistic() {
        AggregatorDataStatistic<Class<?>, Statistic> aggregatorDataStatistic = new AggregatorDataStatistic<>();
        while (true) {
            WrapStatistic wrapStatistic = queue.poll();
            if (wrapStatistic != null) {
                aggregatorDataStatistic.getMap().put(wrapStatistic.getClassOwner(), wrapStatistic.getStatistic());
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
