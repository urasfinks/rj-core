package ru.jamsys.rate.limit;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.component.general.addable.AddableMapItem;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.StatisticsCollectorMap;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.statistic.TimeControllerImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitImpl
        extends TimeControllerImpl
        implements StatisticsCollectorMap<RateLimitItem>, Closable, AddableMapItem<String, RateLimitItem>, RateLimit {

    @Getter
    final Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private boolean active = false;

    @Override
    public void close() {
        setActive(false);
    }

    @Override
    public Map<String, RateLimitItem> getMapStatisticCollectorMap() {
        return map;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    @Override
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("active: ").append(active).append("; ");
        return sb.toString();
    }

    @Override
    public void reset() {
        // Рекомендуется использовать только для тестов
        active = false;
        map.forEach((String key, RateLimitItem rateLimitItem) -> rateLimitItem.reset());
    }

    @Override
    public boolean check(Integer limit) {
        active = true;
        for (String key : map.keySet()) {
            if (!map.get(key).check(limit)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RateLimitItem get(String name, RateLimitItemInstance rateLimitItemInstance) {
        if (!map.containsKey(name)) {
            map.put(name, rateLimitItemInstance.create());
        }
        return map.get(name);
    }
}
