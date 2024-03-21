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

    public RateLimitItem get(String key) {
        if (!map.containsKey(key)) {
            map.put(key, new RateLimitItem());
        }
        return map.get(key);
    }

    public void remove(String key) {
        RateLimitItem rateLimitItem = map.get(key);
        rateLimitItem.setActive(false);
    }

    public boolean check(String key) {
        return get(key).checkTps();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, map, new String[0], (String key, RateLimitItem rateLimitItem) -> {
            if (rateLimitItem.isActive()) {
                result.add(new Statistic(parentTags, parentFields)
                        .addTag("index", key)
                        .addField("max", rateLimitItem.getMax())
                        .addField("tps", rateLimitItem.flushTps()));
            }
        });
        result.add(new Statistic(parentTags, parentFields)
                .addTag("index", getClass().getSimpleName())
                .addField("size", map.size()));
        return result;
    }
}
