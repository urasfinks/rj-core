package ru.jamsys.rate.limit;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class RateLimitMax implements RateLimit {

    @Getter
    @Setter
    private boolean active = false;

    @Setter
    @Getter
    private volatile int max = -1;

    public boolean checkLimit(int currentValue) {
        if (currentValue < -1) {
            return false;
        }
        return this.max < 0 || (this.max > 0 && this.max >= currentValue);
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        max = -1;
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
