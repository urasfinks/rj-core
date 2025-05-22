package ru.jamsys.core.rate.limit;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.builder.HashMapBuilder;
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
public class RateLimitPeriodic
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements RateLimit,
        ManagerElement, CascadeKey {

    private final AtomicInteger tpu = new AtomicInteger(0);

    private final TimeUnit period;

    @Getter
    private final String periodName;

    private final AtomicLong nextTimeFlush = new AtomicLong(0);

    private String nextTimeFlushFormat = "";

    @Getter
    private final String ns;

    private final RateLimitRepositoryProperty property = new RateLimitRepositoryProperty();

    private final PropertyDispatcher<Integer> propertyDispatcher;

    private RateLimitPeriodic(String ns, TimeUnit period) {
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

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                ;
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
        statistic.addHeader("period", periodName);
        statistic.addHeader("max", property.getMax());
        if (nextTimeFlush.get() <= curTime) {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(curTime);
            period.addValue(now, 1);
            long timeInMs = now.getTimeInMillis();
            nextTimeFlush.set(timeInMs);
            nextTimeFlushFormat = UtilDate.msFormat(timeInMs);
            statistic.addHeader("tpu", tpu.getAndSet(0));
            statistic.addHeader("flushed", true);
        } else {
            statistic.addHeader("tpu", tpu.get());
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

    public static Manager.Configuration<RateLimit> getInstanceConfigure(String ns, TimeUnit timeUnit) {
        return getInstanceConfigure(App.context, ns, timeUnit);
    }

    public static Manager.Configuration<RateLimit> getInstanceConfigure(
            ApplicationContext applicationContext,
            String ns,
            TimeUnit timeUnit
    ) {
        return App.get(Manager.class, applicationContext).configureGeneric(
                RateLimit.class,
                ns,
                ns1 -> new RateLimitPeriodic(ns1, timeUnit)
        );
    }

}
