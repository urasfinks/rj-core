package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.EnvelopManagerObject;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.promise.PromiseTaskWithResource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Пул, который предоставляет освободившиеся объекты для задач PromiseTaskPool
// В потоке исполнения задачи - совершается действие с освободившимся объектом и на вход подаётся результат
// Аргументы для действия над ресурсом задаются либо при инициализации задачи, либо непосредственно перед запуском

public abstract class AbstractPoolResource<RA, RR, PI extends Completable & ExpirationMsMutable & Resource<RA, RR>>
        extends AbstractPool<RA, RR, PI> {

    //private final BrokerManager2 brokerManager; //PromiseTaskWithResource<RA, RR, PI>

    final private EnvelopManagerObject<Broker<PromiseTaskWithResource>> broker;

    @SuppressWarnings("all")
    public AbstractPoolResource(String name, int min, Class<PI> cls) {
        super(name, min, cls);
        broker = App.context.getBean(BrokerManager.class).get(getName(), PromiseTaskWithResource.class);

    }

    public void addPromiseTaskPool(PromiseTaskWithResource<RA, RR, PI> promiseTaskWithResource) throws Exception {
        broker.get().add(new ExpirationMsImmutableEnvelope<>(promiseTaskWithResource, promiseTaskWithResource.getPromise().getExpiryRemainingMs()));
        if (!addIfPoolEmpty()) {
            onParkUpdate();
        }
    }

    @Override
    public void onParkUpdate() {
        if (!broker.get().isEmpty() && !parkQueue.isEmpty()) {
            PI poolItem = parkQueue.pollLast();
            if (poolItem != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskWithResource> envelope = broker.get().pollLast();
                if (envelope != null) {
                    updateParkStatistic();
                    envelope.getValue().start(new PoolItemEnvelope<>(this, poolItem));
                } else {
                    complete(poolItem, null);
                }
            }
        }
    }

}
