package ru.jamsys.core.rate.limit.item;

import org.springframework.lang.Nullable;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitItemTps implements RateLimitItem {

    private final AtomicInteger tps = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(-1);

    @Override
    public boolean check(@Nullable Integer limit) {
        int curTps = tps.incrementAndGet();
        return max.get() < 0 || (max.get() > 0 && curTps <= max.get()); // -1 = infinity; 0 = reject
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
        tps.set(0);
        max.set(-1);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("tps", tps.getAndSet(0))
                .addField("max", max.get())
        );
        return result;
    }

    @Override
    public void incrementMax() {
        max.incrementAndGet();
    }

    @Override
    public String getMomentumStatistic() {
        return "{max: " + max.get() + "; tps: " + tps.get() + "}";
    }

}