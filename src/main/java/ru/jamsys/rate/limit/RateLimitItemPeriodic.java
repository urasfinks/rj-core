package ru.jamsys.rate.limit;

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

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    public RateLimitItemPeriodic(Unit period) {
        this.period = period;
    }

    @Override
    public boolean check(@Nullable Integer limit) {
        tpu.incrementAndGet();
        return max.get() < 0 || (max.get() > 0 && tpu.get() <= max.get()); // -1 = infinity; 0 = reject
    }

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
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        long curTime = System.currentTimeMillis();
        Statistic statistic = new Statistic(parentTags, parentFields);
        if (nextTimeFlush.get() <= curTime) {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(curTime);
            period.addValue(now, 1);
            long timeInMillis = now.getTimeInMillis();
            nextTimeFlush.set(timeInMillis);
            nextTimeFlushFormat = Util.msToDataFormat(timeInMillis);
            tpu.set(0);
            statistic.addField("flushed", true);
        } else {
            statistic.addField("flushed", false);
        }

        statistic.addField("tpu", tpu.get());
        statistic.addField("period", period.getName());
        statistic.addField("max", max.get());
        statistic.addField("nextTime", nextTimeFlushFormat);
        result.add(statistic);
        return result;
    }

}
