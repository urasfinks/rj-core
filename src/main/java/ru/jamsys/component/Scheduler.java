package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
import ru.jamsys.scheduler.SchedulerThread;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.statistic.SchedulerStatistic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        Util.riskModifierMap(mapScheduler, new SchedulerType[0], (SchedulerType schedulerType, SchedulerThread schedulerThread) -> schedulerThread.shutdown());
        mapScheduler.clear();
    }

    @Override
    public void flushStatistic() {
        Set<String> set = new HashSet<>();
        Util.riskModifierMap(mapScheduler, new SchedulerType[0], (SchedulerType schedulerType, SchedulerThread schedulerThread) -> set.add(schedulerThread.getName()));
        if (statisticAggregator == null) {
            statisticAggregator = applicationContext.getBean(StatisticAggregator.class);
        }
        statisticAggregator.add(new SchedulerStatistic(set));
        clearNotActive();
    }

    private void clearNotActive() {
        Util.riskModifierMap(mapScheduler, new SchedulerType[0], (SchedulerType schedulerType, SchedulerThread schedulerThread) -> {
            if (!schedulerThread.isActive()) {
                mapScheduler.remove(schedulerType);
            }
        });
    }
}
