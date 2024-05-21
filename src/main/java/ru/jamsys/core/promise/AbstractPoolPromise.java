package ru.jamsys.core.promise;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Пул, который предоставляет освободившиеся объекты для задач PromiseTaskPool
// В потоке исполнения задачи - совершается действие с освободившимся объектом и на вход подаётся результат
// Аргументы для действия над ресурсом задаются либо при инициализации задачи, либо непосредственно перед запуском

public abstract class AbstractPoolPromise<RA, RR, PI extends Completable & ExpirationMsMutable & Resource<RA, RR>>
        extends AbstractPool<RA, RR, PI> {

    private final BrokerManager<PromiseTaskPool<RA, RR, PI>> brokerManager;

    @SuppressWarnings("all")
    public AbstractPoolPromise(String name, int min, Class<PI> cls) {
        super(name, min, cls);
        brokerManager = App.context.getBean(BrokerManager.class);
    }

    public void addPromiseTaskPool(PromiseTaskPool<RA, RR, PI> promiseTaskPool) throws Exception {
        brokerManager.add(name, new ExpirationMsImmutableEnvelope<>(promiseTaskPool, promiseTaskPool.getPromise().getExpiryRemainingMs()));
        addIfPoolEmpty();
        onParkUpdate();
    }

    @Override
    public void onParkUpdate() {
        if (!brokerManager.isEmpty(name) && !parkQueue.isEmpty()) {
            PI poolItem = parkQueue.pollLast();
            if (poolItem != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskPool<RA, RR, PI>> envelope = brokerManager.pollLast(name);
                if (envelope != null) {
                    updateParkStatistic();
                    PoolItemEnvelope<RA, RR, PI> poolItemEnvelope = new PoolItemEnvelope<>(this, poolItem);
                    PromiseTaskPool<RA, RR, PI> value = envelope.getValue();
                    value.start(poolItemEnvelope);
                } else {
                    // Так уж получилось, что нет задач - вернём в пул (возможен конкурентный доступ)
                    complete(poolItem, null);
                }
            }
        }
    }

}
