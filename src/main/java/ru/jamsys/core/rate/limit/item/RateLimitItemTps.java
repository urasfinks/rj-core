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
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitItemTps
        extends AnnotationPropertyExtractor
        implements
        RateLimitItem,
        PropertyUpdater,
        LifeCycleInterface {

    private final AtomicInteger tps = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(1);

    @Getter
    private final String key;

    @SuppressWarnings("all")
    @PropertyName
    private Integer propMax = 1000;

    private final PropertySubscriber propertySubscriber;

    public RateLimitItemTps(String key) {
        this.key = key;
        propertySubscriber  = new PropertySubscriber(
                App.get(ServiceProperty.class),
                this,
                this,
                getKey()
        );
    }

    @Override
    public boolean check() {
        return tps.incrementAndGet() <= max.get();
    }

    @Override
    public int get() {
        return tps.get();
    }

    @Override
    public int max() {
        return max.get();
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
                .addField("max", max.get())
        );
        return result;
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
