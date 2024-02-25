package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LongSummaryStatistics;

@Data
@EqualsAndHashCode(callSuper = false)
public class SchedulerThreadStatistic extends AbstractStatistic {
    //Summary TPS = execTime.count
    LongSummaryStatistics execTime;

    public SchedulerThreadStatistic(
            LongSummaryStatistics execTime
    ) {
        this.execTime = execTime;
    }
}
