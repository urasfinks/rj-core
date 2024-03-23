package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitItem {

    @Getter
    @Setter
    private boolean active = false;

    private final AtomicInteger tps = new AtomicInteger(0);

    private volatile int max = -1;

    public boolean checkMax(int max) {
        if (max < -1) {
            return false;
        }
        return this.max < 0 || (this.max > 0 && this.max >= max);
    }

    public int flushTps() {
        return tps.getAndSet(0);
    }

    public boolean isOverflowTps() {
        return !checkTps();
    }

    public boolean checkTps() {
        boolean result = max < 0 || (max > 0 && tps.get() < max); // -1 = infinity; 0 = reject
        tps.incrementAndGet();
        active = true;
        return result;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        tps.set(0);
        active = true;
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
