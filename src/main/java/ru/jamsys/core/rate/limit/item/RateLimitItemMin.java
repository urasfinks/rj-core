package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Всё сводится к тому, что бы значение не опустилось меньше минимума

public class RateLimitItemMin implements RateLimitItem {

    private final AtomicInteger min = new AtomicInteger(0);

    public RateLimitItemMin(String ns) {

    }

    @Override
    public boolean check(Integer limit) {
        if (limit == null) {
            return false;
        }
        return this.min.get() <= limit;
    }

    @Override
    public void set(Integer limit) {
        this.min.set(limit);
    }

    @Override
    public long get() {
        return min.get();
    }

    @Override
    public void reset() {
        min.set(0);
    }

    @Override
    public void inc() {
        min.incrementAndGet();
    }

    @Override
    public void dec() {
        min.decrementAndGet();
    }

    @Override
    public String getMomentumStatistic() {
        return "{min: " + min.get() + "}";
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields).addField("min", min.get()));
        return result;
    }

}
