package ru.jamsys.rate.limit.v2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitItemMax implements RateLimitItem {

    private final AtomicLong max = new AtomicLong(-1);

    @Override
    public boolean check(Integer limit) {
        if (limit < -1) {
            return false;
        }
        return this.max.get() < 0 || (this.max.get() > 0 && this.max.get() >= limit);
    }

    public void setMax(Integer limit) {
        this.max.set(limit);
    }

    @Override
    public Map<String, Object> flushTps(long curTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("max", max.get());
        return result;
    }
}
