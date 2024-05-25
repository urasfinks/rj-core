package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.ManagerElement;
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

public abstract class AbstractPoolResource<RC, RA, RR, PI extends Completable & ExpirationMsMutable & Resource<RC, RA, RR>>
        extends AbstractPool<RC, RA, RR, PI> {

    //private final BrokerManager2 brokerManager; //PromiseTaskWithResource<RA, RR, PI>

    final private ManagerElement<Broker<PromiseTaskWithResource>, Void> broker;

    @SuppressWarnings("all")
    public AbstractPoolResource(String name, Class<PI> cls) {
        super(name, cls);
        broker = App.context.getBean(BrokerManager.class).get(getName(), PromiseTaskWithResource.class);

    }

    public void addPromiseTaskPool(PromiseTaskWithResource<RC, RA, RR, PI> promiseTaskWithResource) throws Exception {
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
