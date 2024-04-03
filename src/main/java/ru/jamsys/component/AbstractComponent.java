package ru.jamsys.component;

import ru.jamsys.extension.Addable;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TimeController;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public abstract class AbstractComponent<T extends TimeController & Closable & Addable<I, WI>, I, WI> implements ComponentItem<T>, StatisticsCollector {

    private final Map<String, T> mapQueue = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    public WI add(String key, I object) throws Exception {
        return get(key).add(object);
    }

    @SuppressWarnings("unused")
    public T get(String key) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!mapQueue.containsKey(key)) {
            mapQueue.putIfAbsent(key, createComponentItem(key));
        }
        return mapQueue.get(key);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(
                threadEnvelope.getIsWhile(),
                mapQueue,
                new String[0],
                (String key, T queue) -> {
                    parentTags.put(getClass().getSimpleName(), key);
                    List<Statistic> statistics = ((StatisticsCollector) queue).flushAndGetStatistic(parentTags, parentFields, threadEnvelope);
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
