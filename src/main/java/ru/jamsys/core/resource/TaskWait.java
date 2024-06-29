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

// Пул ресурсов хранит очередь задач, которые ждут освободившиеся ресурсы
// При освобождении ресурса происходит передача управления ресурса в задачу
// Пул, который предоставляет освободившиеся ресурсы для задач PromiseTaskPool
// В потоке исполнения задачи - совершается действие с освободившимся ресурсом

public class TaskWait<
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RA, RR>
        >
        extends AbstractPool<RA, RR, PI>
        implements Closable, CheckClassItem {

    private final PoolSettings<PI> poolSettings;

    @SuppressWarnings("all")
    final private Broker<PromiseTaskWithResource> broker;

    private final Class<PI> classItem;

    public TaskWait(String name, PoolSettings<PI> poolSettings, Class<PI> classItem) {
        super(name, poolSettings.getClassPoolItem());
        this.poolSettings = poolSettings;
        this.classItem = classItem;
        broker = App.get(ManagerBroker.class).initAndGet(getName(), PromiseTaskWithResource.class, null);
    }

    @Override
    public PI createPoolItem() {
        PI newPoolItem = App.context.getBean(poolSettings.getClassPoolItem());
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
    public boolean checkCriticalOfExceptionOnComplete(Throwable e) {
        return poolSettings.getIsFatalExceptionOnComplete().apply(e);
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
        // Если пул был пустой, создаётся ресурс и вызывается onParkUpdate()
        // Если же в пуле были ресурсы, то вернётся false и мы самостоятельно запустим onParkUpdate()
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
