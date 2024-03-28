package ru.jamsys.rate.limit.v2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitItemTps implements RateLimitItem {

    private final AtomicLong tps = new AtomicLong(0);

    private final AtomicLong max = new AtomicLong(-1);

    @Override
    public boolean check(Integer limit) {
        tps.incrementAndGet();
        return max.get() < 0 || (max.get() > 0 && tps.get() <= max.get()); // -1 = infinity; 0 = reject
    }

    public void setMax(Integer limit) {
        this.max.set(limit);
    }

    @Override
    public Map<String, Object> flushTps(long curTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("tps", tps.getAndSet(0));
        return result;
    }

}
