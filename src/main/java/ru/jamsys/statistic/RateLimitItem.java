package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitItem {

    @Getter
    @Setter
    private boolean active = true;

    @Getter
    private final AtomicInteger tps = new AtomicInteger(0);

    private volatile int maxTps = -1;

    public boolean check() {
        boolean result = maxTps < 0 || (maxTps > 0 && tps.get() < maxTps); // -1 = infinity; 0 = reject
        tps.incrementAndGet();
        active = true;
        return result;
    }

    public int getMaxTps() {
        return maxTps;
    }

    public void setMaxTps(int maxTps) {
        this.maxTps = maxTps;
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        tps.set(0);
        active = true;
    }
}
