package ru.jamsys.core.component.manager;


import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class BrokerManager<MOI>
        extends AbstractManagerListItem<
        Broker<MOI>,
        ExpirationMsImmutableEnvelope<MOI>,
        DisposableExpirationMsImmutableEnvelope<MOI>
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

    public boolean isEmpty(String key) {
        return get(key).isEmpty();
    }

    public int getOccupancyPercentage(String key) {
        return get(key).getOccupancyPercentage();
    }

    public int size(String key) {
        return get(key).size();
    }

    public ExpirationMsImmutableEnvelope<MOI> pollLast(String key) {
        Broker<MOI> queue = get(key);
        return queue.pollLast();
    }

    public ExpirationMsImmutableEnvelope<MOI> pollFirst(String key) {
        Broker<MOI> queue = get(key);
        return queue.pollFirst();
    }

    public DisposableExpirationMsImmutableEnvelope<MOI> add(String key, MOI element, long curTime, long timeOut) throws Exception {
        Broker<MOI> queue = get(key);
        return queue.add(new ExpirationMsImmutableEnvelope<>(element, timeOut, curTime));
    }

    public DisposableExpirationMsImmutableEnvelope<MOI> add(String key, MOI element, long timeOut) throws Exception {
        Broker<MOI> queue = get(key);
        return queue.add(new ExpirationMsImmutableEnvelope<>(element, timeOut));
    }


    public DisposableExpirationMsImmutableEnvelope<MOI> add(String key, ExpirationMsImmutableEnvelope<MOI> envelope) throws Exception {
        Broker<MOI> queue = get(key);
        return queue.add(envelope);
    }

}
