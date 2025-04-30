package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@FieldNameConstants
public class RateLimitItemPeriodic
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements RateLimitItem,
        ManagerElement, CascadeKey {

    private final AtomicInteger tpu = new AtomicInteger(0);

    private final TimeUnit period;

    @Getter
    private final String periodName;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    private final String ns;

    private final RateLimitItemProperty property = new RateLimitItemProperty();

    private final PropertyDispatcher<Integer> propertyDispatcher;

    public RateLimitItemPeriodic(TimeUnit period, String ns) {
        this.ns = ns;
        this.period = period;
        this.periodName = period.getNameCamel();
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                null,
                property,
                getCascadeKey(ns)
        );
    }

    @Override
    public boolean check() {
        return tpu.incrementAndGet() <= property.getMax(); // -1 = infinity; 0 = reject
    }

    @Override
    public int getCount() {
        return tpu.get();
    }

    @Override
    public int getMax() {
        return property.getMax();
    }

    @Override
    public String getPropertyKey() {
        return getCascadeKey(ns);
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        long curTime = System.currentTimeMillis();
        List<DataHeader> result = new ArrayList<>();
        result.add(flushAndGetStatistic(curTime));
        return result;
    }

    public String getNextTime() {
        return nextTimeFlushFormat;
    }

    public DataHeader flushAndGetStatistic(long curTime) {
        DataHeader statistic = new DataHeader().setBody(ns);
        statistic.put("period", periodName);
        statistic.put("max", property.getMax());
        if (nextTimeFlush.get() <= curTime) {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(curTime);
            period.addValue(now, 1);
            long timeInMs = now.getTimeInMillis();
            nextTimeFlush.set(timeInMs);
            nextTimeFlushFormat = UtilDate.msFormat(timeInMs);
            statistic.put("tpu", tpu.getAndSet(0));
            statistic.put("flushed", true);
        } else {
            statistic.put("tpu", tpu.get());
            statistic.put("flushed", false);
        }
        return statistic;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

}
