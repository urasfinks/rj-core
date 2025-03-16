package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@FieldNameConstants
public class RateLimitItemTps
        extends AnnotationPropertyExtractor<Integer>
        implements
        RateLimitItem,
        LifeCycleInterface {

    private final AtomicInteger tps = new AtomicInteger(0);

    @Getter
    private final String namespace;

    @SuppressWarnings("all")
    @PropertyKey("max")
    @PropertyDescription("Максимальное кол-во итераций")
    private volatile Integer max = 999999;

    private final PropertyDispatcher<Integer> propertyDispatcher;

    public RateLimitItemTps(String namespace) {
        this.namespace = namespace;
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                null,
                this,
                namespace
        );
    }

    @Override
    public boolean check() {
        return tps.incrementAndGet() <= max;
    }

    @Override
    public int getCount() {
        return tps.get();
    }

    @Override
    public int getMax() {
        return max;
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
                .addField("max", max)
        );
        return result;
    }

    @Override
    public boolean isRun() {
        if (propertyDispatcher != null) {
            return propertyDispatcher.isRun();
        }
        return false;
    }

    @Override
    public void run() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdown() {
        propertyDispatcher.shutdown();
    }

}
