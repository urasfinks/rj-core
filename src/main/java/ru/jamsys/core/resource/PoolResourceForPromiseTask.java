package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.sub.ManagerElement;
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

public class PoolResourceForPromiseTask<
        RC,
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RC, RA, RR>
        >
        extends AbstractPool<RC, RA, RR, PI>
        implements Closable, CheckClassItem {

    private final PoolSettings<PI, RC> argument;

    @SuppressWarnings("all")
    final private ManagerElement<Broker<PromiseTaskWithResource>, Void> broker;

    public PoolResourceForPromiseTask(String name, PoolSettings<PI, RC> argument) {
        super(name, argument.getClassPoolItem());
        this.argument = argument;
        broker = App.context.getBean(BrokerManager.class).get(getName(), PromiseTaskWithResource.class);
    }

    @Override
    public PI createPoolItem() {
        PI newPoolItem = App.context.getBean(argument.getClassPoolItem());
        try {
            newPoolItem.constructor(argument.getResourceConstructor());
            return newPoolItem;
        } catch (Throwable e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        return null;
    }

    @Override
    public void closePoolItem(PI poolItem) {
        poolItem.close();
    }

    @Override
    public boolean checkCriticalOfExceptionOnComplete(Exception e) {
        return argument.getCheckExceptionOnComplete().apply(e);
    }

    @Override
    public void close() {
        shutdown();
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
        return true;
    }

    public void addPromiseTaskPool(PromiseTaskWithResource<?> promiseTaskWithResource) {
        broker.get().add(new ExpirationMsImmutableEnvelope<>(promiseTaskWithResource, promiseTaskWithResource.getPromise().getExpiryRemainingMs()));
        if (!addIfPoolEmpty()) {
            onParkUpdate();
        }
    }

    @SuppressWarnings("all")
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
