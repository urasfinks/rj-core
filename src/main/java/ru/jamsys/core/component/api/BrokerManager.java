package ru.jamsys.core.component.api;


import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractComponentCollection;
import ru.jamsys.core.component.item.Broker;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutableEnvelope;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class BrokerManager<MOI>
        extends AbstractComponentCollection<
        Broker<MOI>,
        ExpiredMsImmutableEnvelope<MOI>,
        ExpiredMsImmutableEnvelope<MOI>
                >
        implements StatisticsFlushComponent, KeepAliveComponent {

    private final ApplicationContext applicationContext;

    public BrokerManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Broker<MOI> build(String index) {
        return new Broker<>(index, applicationContext);
    }

    public ExpiredMsImmutableEnvelope<MOI> pollLast(String key) {
        Broker<MOI> queue = get(key);
        return queue.pollLast();
    }

    @SuppressWarnings("unused")
    public ExpiredMsImmutableEnvelope<MOI> pollFirst(String key) {
        Broker<MOI> queue = get(key);
        return queue.pollFirst();
    }

}
