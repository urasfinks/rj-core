package ru.jamsys.rate.limit.v2;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RateLimitImpl implements RateLimit {

    @Getter
    @Setter
    private boolean active = false;

    private final Map<String, RateLimitItem> mapLimit = new LinkedHashMap<>();

    @Override
    public Map<String, Object> flushTps(long curTime) {
        Map<String, Object> result = new HashMap<>();
        mapLimit.forEach((String key, RateLimitItem rateLimitItem) -> result.put(key, rateLimitItem.flushTps(curTime)));
        return result;
    }

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
    public RateLimitItem add(String name, RateLimitItemInstance rateLimitItemInstance) {
        if(!mapLimit.containsKey(name)){
            mapLimit.put(name, rateLimitItemInstance.create());
        }
        return get(name);
    }

    @Override
    public RateLimitItem add(RateLimitName name, RateLimitItemInstance rateLimitItemInstance) {
        return add(name.getName(), rateLimitItemInstance);
    }

    @Override
    public RateLimitItem get(String name) {
        return mapLimit.get(name);
    }

    @Override
    public RateLimitItem get(RateLimitName name) {
        return mapLimit.get(name.getName());
    }

}
