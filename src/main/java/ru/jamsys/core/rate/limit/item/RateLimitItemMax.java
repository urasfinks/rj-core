package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.component.PropertyComponent;
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

// Всё сводится к тому, что бы значение не превысило порог

public class RateLimitItemMax extends PropertyConnector implements RateLimitItem, PropertySubscriberNotify {

    private final AtomicInteger max = new AtomicInteger(1);

    private volatile int cur;

    @SuppressWarnings("all")
    @PropertyName("max")
    private String propMax = "1";

    @Getter
    private final String ns;

    private final Subscriber subscriber;

    public RateLimitItemMax(ApplicationContext applicationContext, String ns) {
        this.ns = ns;
        subscriber = applicationContext.getBean(PropertyComponent.class).getSubscriber(
                this,
                this,
                ns,
                false
        );
    }

    @Override
    public boolean check(Integer limit) {
        if (limit == null) {
            return false;
        }
        cur = limit;
        return this.max.get() >= limit;
    }

    @Override
    public int get() {
        return max.get();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("cur", cur)
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
