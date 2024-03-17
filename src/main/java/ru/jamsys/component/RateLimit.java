package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
public class RateLimit implements StatisticsCollectorComponent {

    Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    Map<String, RateLimitItem> mapSaved = new ConcurrentHashMap<>();

    public RateLimitItem add(String key) {
        if (!map.containsKey(key)) {
            map.put(key, restoreRateLimitItem(key));
        }
        return map.get(key);
    }

    public RateLimitItem restoreRateLimitItem(String key) {
        if (mapSaved.containsKey(key)) {
            return mapSaved.get(key);
        }
        RateLimitItem rateLimitItem = new RateLimitItem();
        mapSaved.put(key, rateLimitItem);
        return rateLimitItem;
    }

    public void remove(String key) {
        map.remove(key);
    }

    public void setMaxTps(String key, int maxTps) {
        add(key).setMaxTps(maxTps);
    }

    public boolean check(String key) {
        if (!map.containsKey(key)) {
            map.put(key, restoreRateLimitItem(key));
        }
        return map.get(key).check();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, map, new String[0], (String key, RateLimitItem threadPool)
                -> result.add(new Statistic(parentTags, parentFields)
                .addTag("index", key)
                .addField("max", threadPool.getMaxTps())
                .addField("tps", threadPool.getTps().getAndSet(0))));
        result.add(new Statistic(parentTags, parentFields)
                .addTag("index", getClass().getSimpleName())
                .addField("saved", mapSaved.size()));
        return result;
    }
}
