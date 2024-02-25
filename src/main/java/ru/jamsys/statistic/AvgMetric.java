package ru.jamsys.statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AvgMetric {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    public void add(Long x) {
        if (x != null) {
            queue.add(x);
        }
    }

    public LongSummaryStatistics flush() {
        List<Long> list = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long poll = queue.poll();
            if (poll != null) {
                list.add(poll);
            } else {
                break;
            }
        }
        return list.stream().mapToLong(Long::intValue).summaryStatistics();
    }

}
