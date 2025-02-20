package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.extension.property.PropertyUpdater;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitItemPeriodic
        extends AnnotationPropertyExtractor
        implements RateLimitItem,
        PropertyUpdater,
        LifeCycleInterface {

    private final AtomicInteger tpu = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(999999);

    private final TimeUnit period;

    @Getter
    private final String periodName;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    @Getter
    private final String key;

    @SuppressWarnings("all")
    @PropertyName
    private Integer propMax = 1000;

    private final PropertySubscriber propertySubscriber;

    public RateLimitItemPeriodic(TimeUnit period, String key) {
        this.key = key;
        this.period = period;
        this.periodName = period.getNameCamel();
        propertySubscriber = new PropertySubscriber(
                App.get(ServiceProperty.class),
                this,
                null,
                getKey()
        );
    }

    @Override
    public boolean check() {
        return tpu.incrementAndGet() <= max.get(); // -1 = infinity; 0 = reject
    }

    @Override
    public int get() {
        return tpu.get();
    }

    @Override
    public int max() {
        return max.get();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
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
            nextTimeFlushFormat = UtilDate.msFormat(timeInMs);
            statistic.addField("tpu", tpu.getAndSet(0));
            statistic.addField("flushed", true);
        } else {
            statistic.addField("tpu", tpu.get());
            statistic.addField("flushed", false);
        }
        return statistic;
    }

    @Override
    public void run() {
        propertySubscriber.run();
    }

    @Override
    public void shutdown() {
        propertySubscriber.shutdown();
    }

    @Override
    public void onPropertyUpdate(String key, Property property) {
        this.max.set(propMax);
    }

}
