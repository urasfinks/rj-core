package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.PropertyRepository;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.PropertyUpdateNotifier;
import ru.jamsys.core.extension.property.PropertyNsAgent;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitItemTps
        extends PropertyRepository
        implements RateLimitItem, PropertyUpdateNotifier, LifeCycleInterface {

    private final AtomicInteger tps = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(1);

    @Getter
    private final String ns;

    @SuppressWarnings("all")
    @PropertyName
    private String propMax = "1000";

    private final PropertyNsAgent propertyNsAgent;

    public RateLimitItemTps(ApplicationContext applicationContext, String ns) {
        this.ns = ns;
        propertyNsAgent = applicationContext.getBean(ServiceProperty.class).getPropertyNsAgent(
                this,
                this,
                ns,
                false
        );
    }

    @Override
    public boolean check(@Nullable Integer limit) {
        return tps.incrementAndGet() <= max.get();
    }

    @Override
    public int get() {
        return max.get();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("tps", tps.getAndSet(0))
                .addField("max", max.get())
        );
        return result;
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedPropAlias) {
        this.max.set(Integer.parseInt(propMax));
    }

    @Override
    public void run() {
        propertyNsAgent.run();
    }

    @Override
    public void shutdown() {
        propertyNsAgent.shutdown();
    }

}
