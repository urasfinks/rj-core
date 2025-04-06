package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.promise.PromiseTaskWithResource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Пул ресурсов хранит очередь задач, которые ждут освободившиеся ресурсы
// При освобождении ресурса происходит передача управления ресурса в задачу
// Пул, который предоставляет освободившиеся ресурсы для задач PromiseTask
// В потоке исполнения задачи - совершается действие с освободившимся ресурсом

// Существует индекс ресурса, например JdbcResource.default
// Далее существует ManagerPoolTaskWait, где в карте хранится по индексу ресурса экземпляр PoolPromiseTaskWaitResource
// PoolPromiseTaskWaitResource - это пул ресурсов с очередью задач, которым для выполнения нужен ресурс


public class PoolPromiseTaskWaitResource<
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RA, RR>
        >
        extends AbstractPool<RA, RR, PI>
        implements ClassEquals, LifeCycleInterface {

    private final PoolSettings<PI> poolSettings;

    @SuppressWarnings("all")
    final private BrokerMemory<PromiseTaskWithResource> broker;

    private final Class<PI> classItem;

    public PoolPromiseTaskWaitResource(
            String key,
            PoolSettings<PI> poolSettings,
            Class<PI> classItem
    ) {
        super(key);
        this.poolSettings = poolSettings;
        this.classItem = classItem;
        broker = App.get(ManagerBroker.class)
                .initAndGet(key, PromiseTaskWithResource.class, null);
    }

    @Override
    public PI createPoolItem() {
        PI newPoolItem = App.context.getBean(poolSettings.getClassPoolItem());
        if (isDebug() && getKey().equals(getDebugKey())) {
            UtilLog.info(getClass(), null)
                    .addHeader("description", "createPoolItem")
                    .addHeader("key", key)
                    .addHeader("poolKey", poolSettings.getKey())
                    .addHeader("result", newPoolItem.getClass().getName())
                    .print();
        }
        try {
            newPoolItem.setArguments(poolSettings.getResourceArguments());
            newPoolItem.run();
            return newPoolItem;
        } catch (Throwable th) {
            App.error(new ForwardException(th));
        }
        return null;
    }

    @Override
    public void closePoolItem(PI poolItem) {
        poolItem.shutdown();
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return poolSettings.getFunctionCheckFatalException().apply(th);
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return this.classItem.equals(classItem);
    }

    public void addPromiseTaskPool(PromiseTaskWithResource<?> promiseTaskWithResource) {
        if (isDebug() && getKey().equals(getDebugKey())) {
            UtilLog.info(getClass(), null)
                    .addHeader("description", "addPromiseTaskPool")
                    .addHeader("poolKey", key)
                    .addHeader("taskWithResourceIndex", promiseTaskWithResource.getIndex())
                    .print();
        }
        setActivity();
        broker.add(new ExpirationMsImmutableEnvelope<>(promiseTaskWithResource, promiseTaskWithResource.getPromise().getExpiryRemainingMs()));
        // Если пул был пустой, создаётся ресурс и вызывается onParkUpdate()
        // Если же в пуле были ресурсы, то вернётся false и мы самостоятельно запустим onParkUpdate()
        // Что бы попытаться найти свободный ресурс и запустить только что добавленную задачу
        if (isAvailablePoolItem()) {
            onParkUpdate();
        }
    }

    @SuppressWarnings("all")
    @Override
    public void onParkUpdate() {
        // Только в том случае если есть задачи в очереди есть ресурсы в парке
        if (!broker.isEmpty() && !isParkQueueEmpty()) {
            PI poolItem = getFromPark();
            if (poolItem != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskWithResource> envelope = broker.pollLast();
                if (envelope != null) {
                    envelope.getValue().start(new PoolItemEnvelope<>(this, poolItem));
                } else {
                    // Если задач более нет, возвращаем плавца в пул
                    completePoolItem(poolItem, null);
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
