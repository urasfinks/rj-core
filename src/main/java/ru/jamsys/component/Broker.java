package ru.jamsys.component;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.component.base.ListItem;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.TimeEnvelope;

@Component
@Lazy
public class Broker<MOI extends BrokerCollectible>
        extends ListItem<
        BrokerQueue<MOI>,
        TimeEnvelope<MOI>,
        TimeEnvelope<MOI>
        >
        implements StatisticsCollectorComponent {

    @Override
    public BrokerQueue<MOI> build(String key) {
        return new BrokerQueue<>(key);
    }

    @SuppressWarnings("unused")
    public TimeEnvelope<MOI> pollLast(String key) {
        BrokerQueue<MOI> queue = get(key);
        return queue.pollLast();
    }

    @SuppressWarnings("unused")
    public TimeEnvelope<MOI> pollFirst(String key) {
        BrokerQueue<MOI> queue = get(key);
        return queue.pollFirst();
    }

}
