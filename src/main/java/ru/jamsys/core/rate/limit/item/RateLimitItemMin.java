package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.extension.PropertyConnector;
import ru.jamsys.core.extension.PropertyName;
import ru.jamsys.core.extension.PropertySubscriberNotify;
import ru.jamsys.core.extension.Subscriber;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Всё сводится к тому, что бы значение не опустилось меньше минимума

public class RateLimitItemMin extends PropertyConnector implements RateLimitItem, PropertySubscriberNotify {

    private final AtomicInteger min = new AtomicInteger(0);

    private volatile int cur;

    @PropertyName("min")
    private String propMin = "0";

    @Getter
    private final String ns;

    private final Subscriber subscriber;

    public RateLimitItemMin(ApplicationContext applicationContext, String ns) {
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
        return this.min.get() <= limit;
    }

    @Override
    public int get() {
        return min.get();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("cur", cur)
                .addField("min", min.get())
        );
        return result;
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {
        this.min.set(Integer.parseInt(propMin));
    }

    public void close() {
        subscriber.unsubscribe();
    }

}
