package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class RateLimitItemMax implements RateLimitItem {

    private final AtomicInteger max = new AtomicInteger(-1);

    @Override
    public boolean check(Integer limit) {
        if (limit == null || limit < -1) {
            return false;
        }
        return this.max.get() < 0 || (this.max.get() > 0 && this.max.get() >= limit);
    }

    @Override
    public void setMax(Integer limit) {
        this.max.set(limit);
    }

    @Override
    public long getMax() {
        return max.get();
    }

    @Override
    public void reset() {
        max.set(-1);
    }

    @Override
    public void incrementMax() {
        max.incrementAndGet();
    }

    @Override
    public String getMomentumStatistic() {
        return "{max: " + max.get() + "}";
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields).addField("max", max.get()));
        return result;
    }

}
