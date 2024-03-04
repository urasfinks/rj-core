package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticsCollector;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Lazy
@Getter
@Setter
public class TaskTiming extends AbstractComponent implements StatisticsCollector {

    Deque<Statistic> deferred = new ConcurrentLinkedDeque<>();
    Map<String, Long> mapIndex = new HashMap<>();

    public void insert(long timeMs, Map<String, AvgMetric> mapTime, Map<String, AtomicLong> mapOperation) {
        mapTime.forEach((String key, AvgMetric metric) -> {
            if (!mapIndex.containsKey(key)) {
                mapIndex.put(key, System.currentTimeMillis());
            }
            deferred.add(new Statistic()
                    .addTag("index", key)
                    .addTag("metric", "Time")
                    .addFields(metric.flush("TimeMs"))
            );
        });
        mapOperation.forEach((String key, AtomicLong metric) -> deferred.add(new Statistic()
                .addTag("index", key)
                .addTag("metric", "Count")
                .addField("count", metric.get())
        ));
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Map<String, Long> cloneIndex = new HashMap<>(mapIndex);
        while (!deferred.isEmpty() && isRun.get()) {
            Statistic statistic = deferred.pollFirst();
            if (statistic != null) {
                String index = statistic.getTags().get("index");
                cloneIndex.remove(index);
                statistic.addTags(parentTags);
                statistic.addFields(parentFields);
                result.add(statistic);
            } else {
                break;
            }
        }
        cloneIndex.forEach((String key, Long value) -> {
//            result.add(new Statistic(parentTags, parentFields)
//                    .addTag("index", key)
//                    .addTag("metric", "TimeMs")
//                    .addField("count", 0)
//            );
            result.add(new Statistic(parentTags, parentFields)
                    .addTag("index", key)
                    .addTag("metric", "Count")
                    .addField("count", 0)
            );
        });
        return result;
    }
}

