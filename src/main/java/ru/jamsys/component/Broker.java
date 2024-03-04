package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.broker.Queue;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class Broker extends AbstractComponent implements StatisticsCollector {

    private final Map<Class<? extends BrokerCollectible>, Queue<? extends BrokerCollectible>> mapQueue = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> void add(Class<T> cls, T object) throws Exception {
        Queue<T> queue = get(cls);
        queue.add(object);
    }

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> Queue<T> get(Class<T> cls) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!mapQueue.containsKey(cls)) {
            mapQueue.putIfAbsent(cls, new BrokerQueue<T>());
        }
        @SuppressWarnings("unchecked")
        Queue<T> queue = (Queue<T>) mapQueue.get(cls);
        return queue;
    }

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> T pollLast(Class<T> c) {
        return get(c).pollLast();
    }

    @SuppressWarnings("unused")
    public <T extends BrokerCollectible> T pollFirst(Class<T> c) {
        return get(c).pollFirst();
    }

    @SafeVarargs
    static <T> Class<T>[] getEmptyType(Class<T>... array) {
        return Arrays.copyOf(array, 0);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(
                isRun,
                mapQueue,
                getEmptyType(),
                (Class<? extends BrokerCollectible> cls, Queue<? extends BrokerCollectible> queue) -> {
                    parentTags.put("index", cls.getSimpleName());
                    List<Statistic> statistics = ((StatisticsCollector) queue).flushAndGetStatistic(parentTags, parentFields, isRun);
                    if (statistics != null) {
                        result.addAll(statistics);
                    }
                }
        );
        return result;
    }
}
