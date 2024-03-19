package ru.jamsys.statistic;

import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@ToString
public class AvgMetric {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    public void add(Long count) {
        if (count != null) {
            queue.add(count);
        }
    }

    public List<Long> get() {
        return List.of(queue.toArray(new Long[0]));
    }

    public Map<String, Object> flush(String prefix) {
        LongSummaryStatistics avg = new LongSummaryStatistics();
        while (!queue.isEmpty()) {
            Long poll = queue.poll();
            if (poll != null) {
                avg.accept(poll);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        long count = avg.getCount();
        result.put(AvgMetricUnit.AVG_COUNT.getName(), avg.getCount());
        result.put(prefix + AvgMetricUnit.MIN.getName(), count == 0 ? 0 : avg.getMin());
        result.put(prefix + AvgMetricUnit.MAX.getName(), count == 0 ? 0 : avg.getMax());
        result.put(prefix + AvgMetricUnit.SUM.getName(), avg.getSum());
        result.put(prefix + AvgMetricUnit.AVG.getName(), avg.getAverage());
        return result;
    }

    public static Map<String, Object> getEmpty(String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(AvgMetricUnit.AVG_COUNT.getName(), 0);
        result.put(prefix + AvgMetricUnit.MIN.getName(), 0);
        result.put(prefix + AvgMetricUnit.MAX.getName(), 0);
        result.put(prefix + AvgMetricUnit.SUM.getName(), 0);
        result.put(prefix + AvgMetricUnit.AVG.getName(), (double) 0);
        return result;
    }

}
