package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@FieldNameConstants
public class RateLimitItemTps
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        RateLimitItem {

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
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        result.add(new DataHeader()
                .setBody(namespace)
                .put("tps", tps.getAndSet(0))
                .put("max", property.getMax())
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
