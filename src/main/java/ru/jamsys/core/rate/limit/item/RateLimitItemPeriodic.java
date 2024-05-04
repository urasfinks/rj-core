package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.template.cron.TimeUnit;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class RateLimitItemPeriodic implements RateLimitItem {

    private final AtomicInteger tpu = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(-1);

    private final TimeUnit period;

    @Getter
    private final String periodName;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    public RateLimitItemPeriodic(TimeUnit period) {
        this.period = period;
        this.periodName = period.getName();
    }

    @Override
    public boolean check(@Nullable Integer limit) {
        int curTpu = tpu.incrementAndGet();
        return max.get() < 0 || (max.get() > 0 && curTpu <= max.get()); // -1 = infinity; 0 = reject
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
        tpu.set(0);
        max.set(-1);
        nextTimeFlush.set(0);
        nextTimeFlushFormat = "";
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        long curTime = System.currentTimeMillis();
        List<Statistic> result = new ArrayList<>();
        result.add(flushAndGetStatistic(curTime, parentTags, parentFields));
        return result;
    }

    public String getNextTime() {
        return nextTimeFlushFormat;
    }

    public Statistic flushAndGetStatistic(long curTime, Map<String, String> parentTags, Map<String, Object> parentFields) {
        Statistic statistic = new Statistic(parentTags, parentFields);
        statistic.addField("period", periodName);
        statistic.addField("max", max.get());
        if (nextTimeFlush.get() <= curTime) {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(curTime);
            period.addValue(now, 1);
            long timeInMillis = now.getTimeInMillis();
            nextTimeFlush.set(timeInMillis);
            nextTimeFlushFormat = Util.msToDataFormat(timeInMillis);
            statistic.addField("tpu", tpu.getAndSet(0));
            statistic.addField("flushed", true);
        } else {
            statistic.addField("tpu", tpu.get());
            statistic.addField("flushed", false);
        }
        return statistic;
    }

    @Override
    public void incrementMax() {
        max.incrementAndGet();
    }

    @Override
    public String getMomentumStatistic() {
        return "{period: " + periodName + "; max: " + max.get() + "; tpu: " + tpu.get() + "}";
    }

}
