package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
import ru.jamsys.scheduler.SchedulerThread;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.statistic.SchedulerStatistic;
import ru.jamsys.statistic.SchedulerThreadStatistic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class Scheduler extends AbstractCoreComponent {

    private final Map<SchedulerType, SchedulerThread> mapScheduler = new ConcurrentHashMap<>();
    private StatisticAggregator statisticAggregator;
    private final ConfigurableApplicationContext applicationContext;

    public Scheduler(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void run() {
        get(SchedulerType.SCHEDULER_STATISTIC_WRITE).add(this::flushStatistic);
        statisticAggregator = applicationContext.getBean(StatisticAggregator.class);
    }

    public SchedulerThread get(SchedulerType schedulerType) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        SchedulerThread schedulerThread = mapScheduler.putIfAbsent(schedulerType, schedulerType.getThread());
        if (schedulerThread == null) {
            schedulerThread = mapScheduler.get(schedulerType);
            schedulerThread.run();
        }
        return schedulerThread;
    }

    @SuppressWarnings("unused")
    public void remove(SchedulerType schedulerType) {
        SchedulerThread schedulerThread = mapScheduler.remove(schedulerType);
        if (schedulerThread != null) {
            schedulerThread.shutdown();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Util.riskModifierMap(
                mapScheduler,
                new SchedulerType[0],
                (SchedulerType schedulerType, SchedulerThread schedulerThread) -> {
                    schedulerThread.shutdown();
                    mapScheduler.remove(schedulerType);
                }
        );
    }

    @Override
    public void flushStatistic() {
        SchedulerStatistic statistic = new SchedulerStatistic();
        Util.riskModifierMap(mapScheduler, new SchedulerType[0], (SchedulerType schedulerType, SchedulerThread schedulerThread) -> {
            statistic.getMap().put(schedulerType, (SchedulerThreadStatistic) schedulerThread.flushAndGetStatistic());
            if (!schedulerThread.isActive()) {
                mapScheduler.remove(schedulerType);
            }
        });
        statisticAggregator.add(getClass(), statistic);
    }

}
