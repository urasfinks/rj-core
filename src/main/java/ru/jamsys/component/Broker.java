package ru.jamsys.component;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.broker.Queue;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class Broker implements StatisticsCollectorComponent {

    private final Map<String, Queue<? extends BrokerCollectible>> mapQueue = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> void add(String key, T object) throws Exception {
        get(key).add(object);
    }

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> Queue<T> get(String key) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!mapQueue.containsKey(key)) {
            mapQueue.putIfAbsent(key, new BrokerQueue<T>(key));
        }
        @SuppressWarnings("unchecked")
        Queue<T> queue = (Queue<T>) mapQueue.get(key);
        return queue;
    }

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> T pollLast(String key) {
        Queue<T> queue = get(key);
        return queue.pollLast();
    }

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> T pollFirst(String key) {
        Queue<T> queue = get(key);
        return queue.pollFirst();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(
                isRun,
                mapQueue,
                new String[0],
                (String key, Queue<? extends BrokerCollectible> queue) -> {
                    parentTags.put("BrokerKey", key);
                    List<Statistic> statistics = ((StatisticsCollector) queue).flushAndGetStatistic(parentTags, parentFields, isRun);
                    if (statistics != null) {
                        result.addAll(statistics);
                    }
                    if (queue.isExpired()) {
                        mapQueue.remove(key);
                        queue.close();
                    }
                }
        );
        return result;
    }

}
