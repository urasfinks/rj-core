package ru.jamsys.core.extension.rate.limit.periodic;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.flat.util.UtilDate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@FieldNameConstants
public class RateLimitPeriodic extends AbstractManagerElement implements PropertyListener {

    private final AtomicInteger tpp = new AtomicInteger(0); // Transaction Per Period

    private TimeUnit period;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    @Getter
    private final String ns;

    private final RateLimitPeriodicRepositoryProperty property = new RateLimitPeriodicRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    public RateLimitPeriodic(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                property,
                getCascadeKey(ns)
        );
        this.period = TimeUnit.valueOf(property.getPeriod());
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                ;
    }

    public boolean check() {
        return tpp.incrementAndGet() <= property.getMax(); // -1 = infinity; 0 = reject
    }

    public int getCount() {
        return tpp.get();
    }

    public int getMax() {
        return property.getMax();
    }

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
        DataHeader statistic = new DataHeader().setBody(getCascadeKey(ns));
        statistic.addHeader("period", period.getNameCamel());
        statistic.addHeader("max", property.getMax());
        if (nextTimeFlush.get() <= curTime) {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(curTime);
            period.addValue(now, 1);
            long timeInMs = now.getTimeInMillis();
            nextTimeFlush.set(timeInMs);
            nextTimeFlushFormat = UtilDate.msFormat(timeInMs);
            statistic.addHeader("tpp", tpp.getAndSet(0));
            statistic.addHeader("flushed", true);
        } else {
            statistic.addHeader("tpp", tpp.get());
            statistic.addHeader("flushed", false);
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

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (key.equals(RateLimitPeriodicRepositoryProperty.Fields.period)) {
            tpp.set(0);
            period = TimeUnit.valueOf(newValue);
        }
    }

    public void setMax(int value) {
        //+.max потому что поле в репозитории называется max
        App.get(ServiceProperty.class)
                .computeIfAbsent(getPropertyKey() + ".max", null)
                .set(value);
    }

}
