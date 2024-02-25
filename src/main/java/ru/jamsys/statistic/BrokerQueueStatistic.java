package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LongSummaryStatistics;

@Data
@EqualsAndHashCode(callSuper = false)
public class BrokerQueueStatistic extends AbstractStatistic {

    int tpsInput;
    int tpsOutput;
    int size;
    LongSummaryStatistics timeInQueue;

    public BrokerQueueStatistic(
            int tpsInput,
            int tpsOutput,
            int size,
            LongSummaryStatistics timeInQueue
    ) {
        this.tpsInput = tpsInput;
        this.tpsOutput = tpsOutput;
        this.size = size;
        this.timeInQueue = timeInQueue;
    }

}
