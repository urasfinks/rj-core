package ru.jamsys.core.component.api;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.item.Broker;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.component.AbstractComponentCollection;
import ru.jamsys.core.statistic.time.TimeControllerMs;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class BrokerManager<MOI extends TimeControllerMs>
        extends AbstractComponentCollection<
        Broker<MOI>,
        TimeEnvelopeMs<MOI>,
        TimeEnvelopeMs<MOI>
                >
        implements StatisticsFlushComponent {

    @Override
    public Broker<MOI> build(String key) {
        return new Broker<>(key);
    }

    public TimeEnvelopeMs<MOI> pollLast(String key) {
        Broker<MOI> queue = getItem(key);
        return queue.pollLast();
    }

    @SuppressWarnings("unused")
    public TimeEnvelopeMs<MOI> pollFirst(String key) {
        Broker<MOI> queue = getItem(key);
        return queue.pollFirst();
    }

}
