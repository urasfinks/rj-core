package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
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
        extends RepositoryPropertiesField
        implements RateLimitItem, PropertyUpdateDelegate, LifeCycleInterface {

    private final AtomicInteger tpu = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(999999);

    private final TimeUnit period;

    @Getter
    private final String periodName;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    @Getter
    private final String ns;

    @SuppressWarnings("all")
    @PropertyName
    private Integer propMax = 1000;

    private final PropertiesAgent propertiesAgent;

    public RateLimitItemPeriodic(TimeUnit period, String ns) {
        this.ns = ns;
        this.period = period;
        this.periodName = period.getNameCamel();
        propertiesAgent = App.get(ServiceProperty.class).getFactory().getPropertiesAgent(
                this,
                this,
                ns,
                false
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
    public void onPropertyUpdate(Map<String, String> mapAlias) {
        this.max.set(propMax);
    }

    @Override
    public void run() {
        propertiesAgent.run();
    }

    @Override
    public void shutdown() {
        propertiesAgent.shutdown();
    }

}
