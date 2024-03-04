package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.handler.ReadTaskHandlerStatistic;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStatisticTaskComponent extends AbstractComponent implements StatisticsCollector {

    Deque<Statistic> deferred = new ConcurrentLinkedDeque<>();
    Map<String, Long> mapIndex = new HashMap<>();

    public AbstractStatisticTaskComponent(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public void insert(Map<String, ReadTaskHandlerStatistic.HStat> time) {
        time.forEach((String key, ReadTaskHandlerStatistic.HStat hStat) -> {
            if (!mapIndex.containsKey(key)) {
                mapIndex.put(key, System.currentTimeMillis());
            }
            //System.out.println(key);
            deferred.add(new Statistic()
                    .addTag("index", key)
                    .addFields(hStat.avgMetric.flush("TimeMs"))
                    .addField("CountOperation", hStat.count.get())
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
                .addField("CountOperation", 0)
        ));
        return result;
    }
}
