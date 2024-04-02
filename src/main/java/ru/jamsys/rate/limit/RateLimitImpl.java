package ru.jamsys.rate.limit;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.thread.ThreadEnvelope;

import java.util.*;

public class RateLimitImpl implements RateLimit {

    @Getter
    @Setter
    private boolean active = false;

    private final Map<String, RateLimitItem> mapLimit = new LinkedHashMap<>();

    @SuppressWarnings({"StringBufferReplaceableByString", "unused"})
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("active: ").append(active).append("; ");
        return sb.toString();
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        active = false;
        mapLimit.forEach((String key, RateLimitItem rateLimitItem) -> rateLimitItem.reset());
    }

    @Override
    public boolean check(Integer limit) {
        active = true;
        for (String key : mapLimit.keySet()) {
            if (!mapLimit.get(key).check(limit)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RateLimitItem get(String name, RateLimitItemInstance rateLimitItemInstance) {
        if (!mapLimit.containsKey(name)) {
            mapLimit.put(name, rateLimitItemInstance.create());
        }
        return mapLimit.get(name);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope) {
        List<Statistic> result = new ArrayList<>();
        mapLimit.forEach((String key, RateLimitItem rateLimitItem) -> {
            HashMap<String, String> stringStringHashMap = new HashMap<>(parentTags);
            stringStringHashMap.put("RateLimitItem", key);
            result.addAll(rateLimitItem.flushAndGetStatistic(stringStringHashMap, parentFields, threadEnvelope));
        });
        return result;
    }
}
