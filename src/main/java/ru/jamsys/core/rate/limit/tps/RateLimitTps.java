package ru.jamsys.core.rate.limit.tps;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@FieldNameConstants
public class RateLimitTps
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        RateLimit, CascadeKey {

    private final AtomicInteger tps = new AtomicInteger(0);

    @Getter
    private final String ns;

    private final RateLimitTpsRepositoryProperty property = new RateLimitTpsRepositoryProperty();

    private final PropertyDispatcher<Integer> propertyDispatcher;

    public RateLimitTps(String ns) {
        this.ns = ns;
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
        return tps.incrementAndGet() <= property.getMax();
    }

    @Override
    public int getCount() {
        return tps.get();
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
        List<DataHeader> result = new ArrayList<>();
        result.add(new DataHeader()
                .setBody(getCascadeKey(ns))
                .addHeader("tps", tps.getAndSet(0))
                .addHeader("max", property.getMax())
        );
        return result;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

    public static ManagerConfiguration<RateLimit> getInstanceConfigure(String key) {
        return getInstanceConfigure(App.context, key);
    }

    public static ManagerConfiguration<RateLimit> getInstanceConfigure(
            ApplicationContext applicationContext,
            String key
    ) {
        return App.get(Manager.class, applicationContext).getManagerConfigurationGeneric(
                RateLimit.class,
                key,
                RateLimitTps::new
        );
    }

}
