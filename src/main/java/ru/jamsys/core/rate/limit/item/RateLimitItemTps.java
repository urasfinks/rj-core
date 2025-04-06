package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@FieldNameConstants
public class RateLimitItemTps
        extends AbstractLifeCycle
        implements
        RateLimitItem,
        LifeCycleInterface {

    private final AtomicInteger tps = new AtomicInteger(0);

    @Getter
    private final String namespace;

    private final RateLimitItemProperty property = new RateLimitItemProperty();

    private final PropertyDispatcher<Integer> propertyDispatcher;

    public RateLimitItemTps(String namespace) {
        this.namespace = namespace;
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                null,
                property,
                namespace
        );
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
    public List<Statistic> flushAndGetStatistic(
            Map<String, String> parentTags,
            Map<String, Object> parentFields,
            AtomicBoolean threadRun
    ) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("tps", tps.getAndSet(0))
                .addField("max", property.getMax())
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

}
