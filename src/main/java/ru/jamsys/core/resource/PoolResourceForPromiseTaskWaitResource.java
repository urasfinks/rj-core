package ru.jamsys.core.resource;

import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.pool.PoolItemCompletable;
import ru.jamsys.core.promise.PromiseTaskWaitResource;

// Пул ресурсов хранит очередь задач, которые ждут освободившиеся ресурсы.
// При освобождении ресурса происходит передача управления ресурса в задачу.
// Пул, который предоставляет освободившиеся ресурсы для задач PromiseTask.
// В потоке исполнения задачи - совершается действие с освободившимся ресурсом.

// Существует индекс ресурса, например JdbcResource.default
// Далее существует ManagerPoolTaskWait, где в карте хранится по индексу ресурса экземпляр PoolPromiseTaskWaitResource.
// PoolPromiseTaskWaitResource - это пул ресурсов с очередью задач, которым для выполнения нужен ресурс.


public class PoolResourceForPromiseTaskWaitResource<T extends AbstractExpirationResource>
        extends AbstractPool<T> {

    @SuppressWarnings("all")
    private final ManagerConfiguration<BrokerMemory<PromiseTaskWaitResource>> brokerMemoryConfiguration;

    public PoolResourceForPromiseTaskWaitResource(String ns) {
        super(ns);
        brokerMemoryConfiguration = ManagerConfiguration.getInstance(
                BrokerMemory.class,
                java.util.UUID.randomUUID().toString(),
                ns,
                promiseTaskWaitResourceBrokerMemory -> promiseTaskWaitResourceBrokerMemory
                        .setup(promiseTaskWaitResource -> promiseTaskWaitResource
                                .getPromise().setError("::drop", new ForwardException(
                                                "drop",
                                                promiseTaskWaitResource
                                        )
                                                .setLine(5)
                                )
                        )
        );
    }

    public void addPromiseTask(PromiseTaskWaitResource<?> promiseTaskWaitResource) {
        markActive();
        brokerMemoryConfiguration
                .get()
                .add(new ExpirationMsImmutableEnvelope<>(
                        promiseTaskWaitResource,
                        promiseTaskWaitResource.getPromise().getRemainingMs()
                ));
        // Если пул был пустой, создаётся ресурс и вызывается onParkUpdate()
        // Если же в пуле были ресурсы, то вернётся false и мы самостоятельно запустим onParkUpdate()
        // Что бы попытаться найти свободный ресурс и запустить только что добавленную задачу
        if (idleIfEmpty()) {
            onParkUpdate();
        }
    }

    @SuppressWarnings("all")
    @Override
    public void onParkUpdate() {
        // Только в том случае если есть задачи в очереди есть ресурсы в парке
        BrokerMemory broker = brokerMemoryConfiguration.get();
        if (!broker.isEmpty() && !isParkQueueEmpty()) {
            T poolItem = this.acquire();
            if (poolItem != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskWaitResource> envelope = broker.pollLast();
                if (envelope != null) {
                    envelope.getValue().onReceiveResource(new PoolItemCompletable<>(this, poolItem));
                } else {
                    // Если задач более нет, возвращаем плавца в пул
                    release(poolItem, null);
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
