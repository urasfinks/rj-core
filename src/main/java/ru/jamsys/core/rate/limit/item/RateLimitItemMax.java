package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Всё сводится к тому, что бы значение не превысило порог

public class RateLimitItemMax implements RateLimitItem {

    private final AtomicInteger max = new AtomicInteger(999999);

    public RateLimitItemMax(String ns) {
    }

    @Override
    public boolean check(Integer limit) {
        if (limit == null) {
            return false;
        }
        return this.max.get() >= limit;
    }

    @Override
    public void set(Integer limit) {
        this.max.set(limit);
    }

    @Override
    public long get() {
        return max.get();
    }

    @Override
    public void reset() {
        max.set(999999);
    }

    @Override
    public void inc() {
        max.incrementAndGet();
    }

    @Override
    public void dec() {
        max.decrementAndGet();
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
