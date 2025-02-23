package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.PropertySubscriber;
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
        LifeCycleInterface {

    private final AtomicInteger tps = new AtomicInteger(0);

    @Getter
    private final String key;

    @SuppressWarnings("all")
    @PropertyName("max")
    private volatile Integer max = 999999;

    private final PropertySubscriber propertySubscriber;

    public RateLimitItemTps(String key) {
        this.key = key;
        propertySubscriber = new PropertySubscriber(
                App.get(ServiceProperty.class),
                null,
                this,
                getKey()
        );
    }

    @Override
    public boolean check() {
        return tps.incrementAndGet() <= max;
    }

    @Override
    public int get() {
        return tps.get();
    }

    @Override
    public int max() {
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
        if (propertySubscriber != null) {
            return propertySubscriber.isRun();
        }
        return false;
    }

    @Override
    public void run() {
        propertySubscriber.run();
    }

    @Override
    public void shutdown() {
        propertySubscriber.shutdown();
    }

}
