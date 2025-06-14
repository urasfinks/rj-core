package ru.jamsys.core.resource;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.pool.AbstractPool;
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

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", getNs())
                .append("propertyDispatcherNs", getPropertyDispatcher().getNs())
                ;
    }

    public void submitPromiseTask(PromiseTaskWaitResource<?> promiseTaskWaitResource) {
        brokerMemoryConfiguration
                .get()
                .add(new ExpirationMsImmutableEnvelope<>(
                        promiseTaskWaitResource,
                        promiseTaskWaitResource.getPromise().getRemainingMs()
                ));
        @SuppressWarnings("all")
        BrokerMemory broker = brokerMemoryConfiguration.get();
        // Если холостой элемент не добавился, по причине того, что пул и так наполнен и в парке есть ресурсы -
        // запускаем процесс
        if (!addIdle() && !isParkQueueEmpty()) {
            onParkUpdate();
        }
    }

    @SuppressWarnings("all")
    @Override
    public void onParkUpdate() {
        // Только в том случае если есть задачи в очереди есть ресурсы в парке
        BrokerMemory broker = brokerMemoryConfiguration.get();
        // Это защита от зацикливания release() -> onParkUpdate() -> release() -> ....
        if (!broker.isEmpty()) {
            // Сначала надо взять ресурс, а только потом задачу, так как если сначала взять задачу и окажется, что
            // её не кому испольнять - ну такое себе
            T resource = this.acquire();
            if (resource != null) {
                //Забираем с конца, что бы никаких штормов
                ExpirationMsImmutableEnvelope<PromiseTaskWaitResource> envelope = broker.poll();
                if (envelope != null) {
                    envelope.getValue().onReceiveResource(resource);
                } else {
                    // Если задач более нет, возвращаем плавца в пул
                    release(resource, null);
                }
            }
        }
    }

    @SuppressWarnings("all")
    @Override
    public boolean forwardResourceWithoutParking(T resource) {
        BrokerMemory broker = brokerMemoryConfiguration.get();
        ExpirationMsImmutableEnvelope<PromiseTaskWaitResource> envelope = broker.poll();
        if (envelope != null) {
            envelope.getValue().onReceiveResource(resource);
            return true;
        }
        return false;
    }

}
