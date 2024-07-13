package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.CheckClassItem;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.ForwardException;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.promise.PromiseTaskWithResource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Пул ресурсов хранит очередь задач, которые ждут освободившиеся ресурсы
// При освобождении ресурса происходит передача управления ресурса в задачу
// Пул, который предоставляет освободившиеся ресурсы для задач PromiseTaskPool
// В потоке исполнения задачи - совершается действие с освободившимся ресурсом

public class PoolTaskWait<
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

    public PoolTaskWait(String name, PoolSettings<PI> poolSettings, Class<PI> classItem) {
        super(name);
        this.poolSettings = poolSettings;
        this.classItem = classItem;
        broker = App.get(ManagerBroker.class).initAndGet(this.getIndex(), PromiseTaskWithResource.class, null);
    }

    @Override
    public PI createPoolItem() {
        PI newPoolItem = App.context.getBean(poolSettings.getClassPoolItem());
        try {
            newPoolItem.constructor(poolSettings.getResourceConstructor());
            return newPoolItem;
        } catch (Throwable th) {
            App.error(new ForwardException(th));
        }
        return null;
    }

    @Override
    public void closePoolItem(PI poolItem) {
        poolItem.close();
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return poolSettings.getFunctionCheckFatalException().apply(th);
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
        // Что бы попытаться найти свободный ресурс и запустить только что добавленную задачу
        if (!addIfPoolEmpty()) {
            onParkUpdate();
        }
    }

    @SuppressWarnings("all")
    @Override
    public void onParkUpdate() {
        if (!broker.isEmpty() && !parkQueue.isEmpty()) {
            PI poolItem = getFromPark();
            if (poolItem != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskWithResource> envelope = broker.pollLast();
                if (envelope != null) {
                    envelope.getValue().start(new PoolItemEnvelope<>(this, poolItem));
                } else {
                    complete(poolItem, null);
                }
            }
        }
    }

    @Override
    public boolean forwardResourceWithoutParking(PI poolItem) {
        ExpirationMsImmutableEnvelope<PromiseTaskWithResource> envelope = broker.pollLast();
        if (envelope != null) {
            envelope.getValue().start(new PoolItemEnvelope<>(this, poolItem));
            return true;
        }
        return false;
    }
}
