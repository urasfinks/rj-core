package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.pool.PoolItemCompletable;
import ru.jamsys.core.pool.Valid;
import ru.jamsys.core.promise.PromiseTaskWaitResource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.function.Function;

// Пул ресурсов хранит очередь задач, которые ждут освободившиеся ресурсы
// При освобождении ресурса происходит передача управления ресурса в задачу
// Пул, который предоставляет освободившиеся ресурсы для задач PromiseTask
// В потоке исполнения задачи - совершается действие с освободившимся ресурсом

// Существует индекс ресурса, например JdbcResource.default
// Далее существует ManagerPoolTaskWait, где в карте хранится по индексу ресурса экземпляр PoolPromiseTaskWaitResource
// PoolPromiseTaskWaitResource - это пул ресурсов с очередью задач, которым для выполнения нужен ресурс


public class PoolResourcePromiseTaskWaitResource<
        T extends ExpirationMsMutable & Valid & LifeCycleInterface & ResourceCheckException
        >
        extends AbstractPool<T>
        implements ManagerElement {

    @SuppressWarnings("all")
    final private Manager.Configuration<BrokerMemory> brokerMemoryConfiguration;

    final private Function<String, T> supplierPoolItem;

    public PoolResourcePromiseTaskWaitResource(
            String ns,
            Function<String, T> supplierPoolItem
    ) {
        super(ns);
        this.supplierPoolItem = supplierPoolItem;
        brokerMemoryConfiguration = App.get(Manager.class).configure(
                BrokerMemory.class,
                ns,
                (ns1) -> new BrokerMemory<PromiseTaskWaitResource<?>>(ns1, App.context, null)
        );
    }

    @Override
    public T createPoolItem() {
        return supplierPoolItem.apply(ns);
    }

    @Override
    public void closePoolItem(T poolItem) {
        poolItem.shutdown();
    }

    @SuppressWarnings("unchecked")
    public void addPromiseTask(PromiseTaskWaitResource<?> promiseTaskWaitResource) {
        markActive();
        brokerMemoryConfiguration
                .get()
                .add(new ExpirationMsImmutableEnvelope<>(
                        promiseTaskWaitResource,
                        promiseTaskWaitResource.getPromise().getExpiryRemainingMs()
                ));
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
        BrokerMemory broker = brokerMemoryConfiguration.get();
        if (!broker.isEmpty() && !isParkQueueEmpty()) {
            T poolItem = get();
            if (poolItem != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskWaitResource> envelope = broker.pollLast();
                if (envelope != null) {
                    envelope.getValue().onReceiveResource(new PoolItemCompletable<>(this, poolItem));
                } else {
                    // Если задач более нет, возвращаем плавца в пул
                    releasePoolItem(poolItem, null);
                }
            }
        }
    }

    @SuppressWarnings("all")
    @Override
    public boolean forwardResourceWithoutParking(T poolItem) {
        ExpirationMsImmutableEnvelope<PromiseTaskWaitResource> envelope = brokerMemoryConfiguration.get().pollLast();
        if (envelope != null) {
            envelope.getValue().onReceiveResource(new PoolItemCompletable<>(this, poolItem));
            return true;
        }
        return false;
    }

}
