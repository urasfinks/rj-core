package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.CheckClassItem;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.promise.PromiseTaskWithResource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Пул, который предоставляет освободившиеся объекты для задач PromiseTaskPool
// В потоке исполнения задачи - совершается действие с освободившимся объектом и на вход подаётся результат
// Аргументы для действия над ресурсом задаются либо при инициализации задачи, либо непосредственно перед запуском

public class PoolTask<
        RC,
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RC, RA, RR>
        >
        extends AbstractPool<RC, RA, RR, PI>
        implements Closable, CheckClassItem {

    private final PoolSettings<PI, RC> poolSettings;

    @SuppressWarnings("all")
    final private Broker<PromiseTaskWithResource> broker;

    private final Class<PI> classItem;

    public PoolTask(String name, PoolSettings<PI, RC> poolSettings, Class<PI> classItem) {
        super(name, poolSettings.getClassPoolItem());
        this.poolSettings = poolSettings;
        this.classItem = classItem;
        broker = App.get(ManagerBroker.class).initAndGet(getName(), PromiseTaskWithResource.class, null);
    }

    @Override
    public PI createPoolItem() {
        PI newPoolItem = App.get(poolSettings.getClassPoolItem());
        try {
            newPoolItem.constructor(poolSettings.getResourceConstructor());
            return newPoolItem;
        } catch (Throwable e) {
            App.error(e);
        }
        return null;
    }

    @Override
    public void closePoolItem(PI poolItem) {
        poolItem.close();
    }

    @Override
    public boolean checkCriticalOfExceptionOnComplete(Exception e) {
        return poolSettings.getCheckExceptionOnComplete().apply(e);
    }

    @Override
    public void close() {
        isRun.set(false);
        shutdown();
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
        return this.classItem.equals(classItem);
    }

    public void addPromiseTaskPool(PromiseTaskWithResource<?> promiseTaskWithResource) {
        active();
        broker.add(new ExpirationMsImmutableEnvelope<>(promiseTaskWithResource, promiseTaskWithResource.getPromise().getExpiryRemainingMs()));
        if (!addIfPoolEmpty()) {
            onParkUpdate();
        }
    }

    @SuppressWarnings("all")
    @Override
    public void onParkUpdate() {
        if (!broker.isEmpty() && !parkQueue.isEmpty()) {
            PI poolItem = parkQueue.pollLast();
            if (poolItem != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskWithResource> envelope = broker.pollLast();
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
