package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.scheduler.SchedulerThread;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.scheduler.SchedulerStatistic;

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
        get(SchedulerType.SCHEDULER_STATISTIC_WRITE).add(this::flushStatistic);
    }

    public SchedulerThread get(SchedulerType schedulerType) {
        if (!mapScheduler.containsKey(schedulerType)) {
            mapScheduler.put(schedulerType, schedulerType.getThread());
        }
        return mapScheduler.get(schedulerType);
    }

    @SuppressWarnings("unused")
    public void remove(SchedulerType schedulerType) {
        SchedulerThread remove = mapScheduler.remove(schedulerType);
        remove.shutdown();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        SchedulerType[] objects = mapScheduler.keySet().toArray(new SchedulerType[0]);
        for (SchedulerType object : objects) {
            SchedulerThread schedulerCustom = mapScheduler.get(object);
            if (schedulerCustom != null) {
                schedulerCustom.shutdown();
            }
        }
        mapScheduler.clear();
    }

    @Override
    public void flushStatistic() {
        Set<String> set = new HashSet<>();
        for (SchedulerType schedulerType : mapScheduler.keySet()) {
            set.add(schedulerType.getName());
        }
        SchedulerStatistic schedulerStatistic = new SchedulerStatistic(set);
        if (statisticAggregator == null) {
            statisticAggregator = applicationContext.getBean(StatisticAggregator.class);
        }
        statisticAggregator.add(schedulerStatistic);
        clearExpired();
    }

    private void clearExpired() {
        SchedulerType[] objects = mapScheduler.keySet().toArray(new SchedulerType[0]);
        for (SchedulerType object : objects) {
            SchedulerThread schedulerCustom = mapScheduler.get(object);
            if (!schedulerCustom.isActive()) {
                mapScheduler.remove(object);
            }
        }
    }
}
