package ru.jamsys.core.statistic;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@ToString
public class AvgMetric {

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Flush {
        long min;
        long max;
        long count;
        long sum;
        double avg;
    }

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    public void add(Long count) {
        if (count != null) {
            queue.add(count);
        }
    }

    public List<Long> get() {
        return List.of(queue.toArray(new Long[0]));
    }

    public LongSummaryStatistics flush() {
        LongSummaryStatistics avg = new LongSummaryStatistics();
        while (!queue.isEmpty()) {
            Long poll = queue.poll();
            if (poll != null) {
                avg.accept(poll);
            }
        }
        return avg;
    }

    public Flush flushInstance() {
        LongSummaryStatistics longSummaryStatistics = new LongSummaryStatistics();
        while (!queue.isEmpty()) {
            Long poll = queue.poll();
            if (poll != null) {
                longSummaryStatistics.accept(poll);
            }
        }
        long count = longSummaryStatistics.getCount();
        return new Flush()
                .setCount(count)
                .setMin(count == 0 ? 0 : longSummaryStatistics.getMin())
                .setMax(count == 0 ? 0 : longSummaryStatistics.getMax())
                .setSum(longSummaryStatistics.getSum())
                .setAvg(longSummaryStatistics.getAverage())
                ;
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
        result.put(AvgMetricUnit.COUNT.getNameCache(), avg.getCount());
        result.put(prefix + AvgMetricUnit.MIN.getNameCache(), count == 0 ? 0 : avg.getMin());
        result.put(prefix + AvgMetricUnit.MAX.getNameCache(), count == 0 ? 0 : avg.getMax());
        result.put(prefix + AvgMetricUnit.SUM.getNameCache(), avg.getSum());
        result.put(prefix + AvgMetricUnit.AVG.getNameCache(), avg.getAverage());
        return result;
    }

    @SuppressWarnings("unused")
    public static Map<String, Object> getEmpty(String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(AvgMetricUnit.COUNT.getNameCache(), 0);
        result.put(prefix + AvgMetricUnit.MIN.getNameCache(), 0);
        result.put(prefix + AvgMetricUnit.MAX.getNameCache(), 0);
        result.put(prefix + AvgMetricUnit.SUM.getNameCache(), 0);
        result.put(prefix + AvgMetricUnit.AVG.getNameCache(), (double) 0);
        return result;
    }

}
