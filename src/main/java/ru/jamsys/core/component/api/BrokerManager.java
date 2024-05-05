package ru.jamsys.core.component.api;


import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractComponentCollection;
import ru.jamsys.core.component.item.Broker;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class BrokerManager<MOI>
        extends AbstractComponentCollection<
        Broker<MOI>,
        TimeEnvelopeMs<MOI>,
        TimeEnvelopeMs<MOI>
                >
        implements StatisticsFlushComponent {

    private final ApplicationContext applicationContext;

    public BrokerManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Broker<MOI> build(String key) {
        return new Broker<>(key, applicationContext);
    }

    public TimeEnvelopeMs<MOI> pollLast(String key) {
        Broker<MOI> queue = get(key);
        return queue.pollLast();
    }

    @SuppressWarnings("unused")
    public TimeEnvelopeMs<MOI> pollFirst(String key) {
        Broker<MOI> queue = get(key);
        return queue.pollFirst();
    }

}
