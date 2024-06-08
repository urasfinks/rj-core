package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.flat.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitItemPeriodic implements RateLimitItem {

    private final AtomicInteger tpu = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(999999);

    private final TimeUnit period;

    @Getter
    private final String periodName;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    public RateLimitItemPeriodic(TimeUnit period, String ns) {
        this.period = period;
        this.periodName = period.getName();
    }

    @Override
    public boolean check(@Nullable Integer limit) {
        return tpu.incrementAndGet() <= max.get(); // -1 = infinity; 0 = reject
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
        tpu.set(0);
        max.set(999999);
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
            long timeInMs = now.getTimeInMillis();
            nextTimeFlush.set(timeInMs);
            nextTimeFlushFormat = Util.msToDataFormat(timeInMs);
            statistic.addField("tpu", tpu.getAndSet(0));
            statistic.addField("flushed", true);
        } else {
            statistic.addField("tpu", tpu.get());
            statistic.addField("flushed", false);
        }
        return statistic;
    }

    @Override
    public void inc() {
        max.incrementAndGet();
    }

    @Override
    public void dec() {
        max.decrementAndGet();
    }

}
