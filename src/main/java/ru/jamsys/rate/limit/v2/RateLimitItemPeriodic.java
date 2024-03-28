package ru.jamsys.rate.limit.v2;

import ru.jamsys.template.cron.Unit;
import ru.jamsys.util.Util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
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
    public boolean check(Integer limit) {
        tpu.incrementAndGet();
        return max.get() < 0 || (max.get() > 0 && tpu.get() <= max.get()); // -1 = infinity; 0 = reject
    }

    public void setMax(Integer limit) {
        this.max.set(limit);
    }

    @Override
    public Map<String, Object> flushTps(long curTime) {
        Map<String, Object> result = new HashMap<>();
        if (nextTimeFlush.get() <= curTime) {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(curTime);
            period.addValue(now, 1);
            long timeInMillis = now.getTimeInMillis();
            nextTimeFlush.set(timeInMillis);
            nextTimeFlushFormat = Util.msToDataFormat(timeInMillis);
            tpu.set(0);
            result.put("flushed", true);
        } else {
            result.put("flushed", false);
        }

        result.put("tpu", tpu.get());
        result.put("period", period.getName());
        result.put("max", max.get());
        result.put("nextTime", nextTimeFlushFormat);

        return result;
    }

}
