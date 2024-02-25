package ru.jamsys.statistic;

import ru.jamsys.scheduler.SchedulerType;

import java.util.List;

public class SchedulerStatistic extends MapStatistic<SchedulerType, SchedulerThreadStatistic> {

    @Override
    public List<StatisticEntity> getStatisticEntity() {
        List<StatisticEntity> result = super.getStatisticEntity();
        getMap().forEach((SchedulerType schedulerType, SchedulerThreadStatistic schedulerThreadStatistic) -> result.add(
                new StatisticEntity()
                        .addTag("scheduler", schedulerType.getName())
                        .addFields(schedulerThreadStatistic.getAsMap())
        ));
        return result;
    }
}
