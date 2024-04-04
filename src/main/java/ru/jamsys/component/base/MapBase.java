package ru.jamsys.component.base;

import ru.jamsys.extension.Closable;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TimeController;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MapBase<T extends Closable & TimeController>
        implements
        StatisticsCollector,
        KeepAlive,
        ItemBuilder<T> {

    protected final Map<String, T> map = new ConcurrentHashMap<>();

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(
                threadEnvelope.getIsWhile(),
                map,
                new String[0],
                (String key, T element) -> {
                    LinkedHashMap<String, String> newParentTags = new LinkedHashMap<>(parentTags);
                    newParentTags.put(getClass().getSimpleName(), key);
                    List<Statistic> statistics = ((StatisticsCollector) element).flushAndGetStatistic(newParentTags, parentFields, threadEnvelope);
                    if (statistics != null) {
                        result.addAll(statistics);
                    }
                }
        );
        return result;
    }

    @Override
    public void keepAlive(ThreadEnvelope threadEnvelope) {
        Util.riskModifierMap(
                threadEnvelope.getIsWhile(),
                map,
                new String[0],
                (String key, T element) -> {
                    if (element.isExpired()) {
                        map.remove(key);
                        element.close();
                    }
                }
        );
    }

    @SuppressWarnings("unused")
    public T get(String key) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!map.containsKey(key)) {
            map.putIfAbsent(key, build(key));
        }
        return map.get(key);
    }

    //Используйте только для тестирования
    @SuppressWarnings("unused")
    public void reset() {
        map.clear();
    }

    @SuppressWarnings("unused")
    public boolean contains(String key) {
        return map.containsKey(key);
    }

}
