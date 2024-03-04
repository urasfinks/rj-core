package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticsCollector;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
@Getter
@Setter
public class TaskTiming extends AbstractComponent implements StatisticsCollector {

    Deque<Statistic> deferred = new ConcurrentLinkedDeque<>();
    Map<String, Long> mapIndex = new HashMap<>();

    public TaskTiming(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public void insert(Map<String, AvgMetric> taskTime, Map<String, AvgMetric> taskHandlerTime) {
        taskTime.forEach((String key, AvgMetric metric) -> {
            if (!mapIndex.containsKey(key)) {
                mapIndex.put(key, System.currentTimeMillis());
            }
            deferred.add(new Statistic()
                    .addTag("index", key)
                    .addFields(metric.flush("TimeMs"))
            );
        });
        taskHandlerTime.forEach((String key, AvgMetric metric) -> {
            if (!mapIndex.containsKey(key)) {
                mapIndex.put(key, System.currentTimeMillis());
            }
            deferred.add(new Statistic()
                    .addTag("index", key)
                    .addFields(metric.flush("TimeMs"))
            );
        });
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Map<String, Long> cloneMapIndex = new HashMap<>(mapIndex);
        while (!deferred.isEmpty() && isRun.get()) {
            Statistic statistic = deferred.pollFirst();
            if (statistic != null) {
                String index = statistic.getTags().get("index");
                cloneMapIndex.remove(index);
                statistic.addTags(parentTags);
                statistic.addFields(parentFields);
                result.add(statistic);
            } else {
                break;
            }
        }
        cloneMapIndex.forEach((String key, Long value) -> result.add(new Statistic(parentTags, parentFields)
                .addTag("index", key)
                .addFields(AvgMetric.getEmpty("TimeMs"))
        ));
        return result;
    }
}

