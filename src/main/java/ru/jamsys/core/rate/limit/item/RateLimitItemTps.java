package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyName;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitItemTps extends PropertyConnector implements RateLimitItem, PropertySubscriberNotify {

    private final AtomicInteger tps = new AtomicInteger(0);

    private final AtomicInteger max = new AtomicInteger(1);

    @Getter
    private final String ns;

    @SuppressWarnings("all")
    @PropertyName("max")
    private String propMax = "1";

    private final Subscriber subscriber;

    public RateLimitItemTps(ApplicationContext applicationContext, String ns) {
        this.ns = ns;
        subscriber = applicationContext.getBean(ServiceProperty.class).getSubscriber(
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
    public void onPropertyUpdate(Set<String> updatedProp) {
        this.max.set(Integer.parseInt(propMax));
    }

    public void close() {
        subscriber.unsubscribe();
    }

}
