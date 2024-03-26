package ru.jamsys.rate.limit;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitTps implements RateLimit {
    @Getter
    @Setter
    private boolean active = false;

    private final AtomicInteger tps = new AtomicInteger(0);

    @Getter
    private volatile int max = -1;

    public boolean isOverflowTps() {
        return !checkTps();
    }

    public boolean checkTps() {
        tps.incrementAndGet();
        active = true;
        return max < 0 || (max > 0 && tps.get() <= max); // -1 = infinity; 0 = reject
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        tps.set(0);
        active = false;
    }

    @Override
    public Map<String, Object> flush() {
        Map<String, Object> result = new HashMap<>();
        result.put("class", getClass().getSimpleName());
        result.put("tps", tps.getAndSet(0));
        result.put("max", max);
        return result;
    }

    @SuppressWarnings({"StringBufferReplaceableByString", "unused"})
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("active: ").append(active).append("; ");
        sb.append("tps: ").append(tps.get()).append("; ");
        sb.append("max: ").append(max).append("; ");
        return sb.toString();
    }
}
