package ru.jamsys.rate.limit;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitMax implements RateLimit {

    @Getter
    @Setter
    private boolean active = false;

    private final AtomicInteger max = new AtomicInteger(-1);

    public void setMax(int max) {
        this.max.set(max);
    }

    public int getMax() {
        return max.get();
    }

    public boolean checkLimit(int currentValue) {
        if (currentValue < -1) {
            return false;
        }
        return this.max.get() < 0 || (this.max.get() > 0 && this.max.get() >= currentValue);
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        max.set(-1);
        active = false;
    }

    @Override
    public Map<String, Object> flush() {
        Map<String, Object> result = new HashMap<>();
        result.put("class", getClass().getSimpleName());
        result.put("limit", max);
        return result;
    }

    @SuppressWarnings({"StringBufferReplaceableByString", "unused"})
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("active: ").append(active).append("; ");
        sb.append("limit: ").append(max).append("; ");
        return sb.toString();
    }
}
