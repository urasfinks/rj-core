package ru.jamsys.component;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.TimeEnvelope;

@Component
@Lazy
public class Broker<T extends BrokerCollectible> extends AbstractMapComponent<BrokerQueue<T>, T, TimeEnvelope<T>> implements StatisticsCollectorComponent {

    @Override
    public BrokerQueue<T> createComponentItem(String key) {
        return new BrokerQueue<>(key);
    }

    @SuppressWarnings("unused")
    public T pollLast(String key) {
        BrokerQueue<T> queue = get(key);
        return queue.pollLast();
    }

    @SuppressWarnings("unused")
    public T pollFirst(String key) {
        BrokerQueue<T> queue = get(key);
        return queue.pollFirst();
    }

}
