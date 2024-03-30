package ru.jamsys.rate.limit.item;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.template.cron.Unit;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitItemPeriodic implements RateLimitItem {

    private final AtomicLong tpu = new AtomicLong(0);

    private final AtomicLong max = new AtomicLong(-1);

    private final Unit period;

    @Getter
    private final String periodName;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    public RateLimitItemPeriodic(Unit period) {
        this.period = period;
        this.periodName = period.getName();
    }

    @Override
    public boolean check(@Nullable Integer limit) {
        tpu.incrementAndGet();
        return max.get() < 0 || (max.get() > 0 && tpu.get() <= max.get()); // -1 = infinity; 0 = reject
    }

    public void setMax(Long limit) {
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
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        long curTime = System.currentTimeMillis();
        List<Statistic> result = new ArrayList<>();
        result.add(flushAndGetStatistic(curTime, parentTags, parentFields));
        return result;
    }

    public String getNextTime(){
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

}
