package ru.jamsys.core.statistic;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ConcurrentLinkedQueue;

@ToString
public class AvgMetric {

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Statistic {
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

    public LongSummaryStatistics flushLongSummaryStatistics() {
        LongSummaryStatistics longSummaryStatistics = new LongSummaryStatistics();
        while (!queue.isEmpty()) {
            Long poll = queue.poll();
            if (poll != null) {
                longSummaryStatistics.accept(poll);
            }
        }
        return longSummaryStatistics;
    }

    public Statistic flushStatistic() {
        LongSummaryStatistics longSummaryStatistics = flushLongSummaryStatistics();
        long count = longSummaryStatistics.getCount();
        return new Statistic()
                .setCount(count)
                .setMin(count == 0 ? 0 : longSummaryStatistics.getMin())
                .setMax(count == 0 ? 0 : longSummaryStatistics.getMax())
                .setSum(longSummaryStatistics.getSum())
                .setAvg(longSummaryStatistics.getAverage())
                ;
    }

    @SuppressWarnings("unused")
    @JsonValue
    Object getValue() {
        return flushStatistic();
    }

}
