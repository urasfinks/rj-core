package ru.jamsys.statistic;

import lombok.ToString;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@ToString
public class AvgMetric {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    public void add(Long x) {
        if (x != null) {
            queue.add(x);
        }
    }

    public Map<String, Object> flush(String prefix) {
        List<Long> list = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long poll = queue.poll();
            if (poll != null) {
                list.add(poll);
            } else {
                break;
            }
        }
        LongSummaryStatistics avg = list.stream().mapToLong(Long::intValue).summaryStatistics();
        Map<String, Object> result = new LinkedHashMap<>();
        long count = avg.getCount();
        result.put("Count", avg.getCount());
        result.put(prefix + "Min", count == 0 ? 0 : avg.getMin());
        result.put(prefix + "Max", count == 0 ? 0 : avg.getMax());
        result.put(prefix + "Sum", avg.getSum());
        result.put(prefix + "Avg", avg.getAverage());
        return result;
    }

}
