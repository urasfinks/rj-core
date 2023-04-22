package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Procedure;
import ru.jamsys.scheduler.SchedulerCustom;
import ru.jamsys.scheduler.SchedulerGlobal;
import ru.jamsys.scheduler.SchedulerStatistic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class Scheduler extends AbstractCoreComponent {

    private final Map<String, SchedulerCustom> mapScheduler = new ConcurrentHashMap<>();
    private StatisticAggregator statisticAggregator;
    private final ConfigurableApplicationContext applicationContext;

    public Scheduler(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        add(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_WRITE, this::flushStatistic);
    }

    @SuppressWarnings("unused")
    public SchedulerCustom add(SchedulerGlobal schedulerGlobal, Procedure procedure) {
        return add(schedulerGlobal.getNameScheduler(), procedure, schedulerGlobal.getPeriodMillis());
    }

    public SchedulerCustom add(String name, Procedure procedure, long periodMillis) {
        if(!mapScheduler.containsKey(name)){
            mapScheduler.put(name, new SchedulerCustom(name, periodMillis));
        }
        SchedulerCustom schedulerCustom = mapScheduler.get(name);
        if (procedure != null) {
            schedulerCustom.add(procedure);
        }
        return schedulerCustom;
    }

    @SuppressWarnings("unused")
    public void remove(SchedulerGlobal schedulerGlobal, Procedure procedure) {
        remove(schedulerGlobal.getNameScheduler(), procedure);
    }

    public void remove(String name, Procedure procedure) {
        SchedulerCustom schedulerCustom = mapScheduler.get(name);
        schedulerCustom.remove(procedure);
        if (!schedulerCustom.isActive()) {
            mapScheduler.remove(name);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        String[] objects = mapScheduler.keySet().toArray(new String[0]);
        for (String object : objects) {
            SchedulerCustom schedulerCustom = mapScheduler.get(object);
            if (schedulerCustom != null) {
                schedulerCustom.shutdown();
            }

        }
    }

    @Override
    public void flushStatistic() {
        SchedulerStatistic schedulerStatistic = new SchedulerStatistic(mapScheduler.keySet());
        if (statisticAggregator == null) {
            statisticAggregator = applicationContext.getBean(StatisticAggregator.class);
        }
        statisticAggregator.add(schedulerStatistic);
    }
}
