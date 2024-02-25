package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;

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

    public Map<String, Object> getAsMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        long count = execTime.getCount();
        result.put("count", execTime.getCount());
        result.put("minTime", count == 0 ? 0 : execTime.getMin());
        result.put("maxTime", count == 0 ? 0 : execTime.getMax());
        result.put("sumTime", execTime.getSum());
        result.put("averageTime", execTime.getAverage());
        return result;
    }
}
