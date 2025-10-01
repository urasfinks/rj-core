package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.rate.limit.tps.RateLimitTps;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.AbstractPromiseTask;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class PoolThreadExecutePromiseTask extends AbstractPoolPrivate {

    AtomicInteger counter = new AtomicInteger(1);

    private final ManagerConfiguration<RateLimitTps> rateLimitConfiguration;

    @SuppressWarnings("all")
    private final ManagerConfiguration<BrokerMemory<AbstractPromiseTask>> brokerMemoryConfiguration;

    public PoolThreadExecutePromiseTask(String ns, String key) {
        super(ns, key);
        // Для каждого ThreadPoolExecutePromiseTask должен быть свой RateLimitTps, поэтому uuid
        rateLimitConfiguration = ManagerConfiguration.getInstance(
                getCascadeKey(ns),
                java.util.UUID.randomUUID().toString(),
                RateLimitTps.class,
                null
        );
        brokerMemoryConfiguration = ManagerConfiguration.getInstance(
                getCascadeKey(ns),
                java.util.UUID.randomUUID().toString(),
                BrokerMemory.class,
                managerElement -> managerElement
                        .setup(promiseTask -> promiseTask
                                .getPromise().setError("::drop", new RuntimeException())
                        )
        );
        setup(
                ThreadExecutePromiseTask.class,
                threadExecutePromiseTask -> threadExecutePromiseTask.setup(
                        this,
                        rateLimitConfiguration,
                        counter.getAndIncrement()
                )
        );
    }

    public void addPromiseTask(AbstractPromiseTask promiseTask) {
        // Контроль TPS управляется в реализации самого потока, тут ограничивать ничего не надо.
        // Есть задача onError, которая может вызываться после вызова timeOut. У обещания больше нет остаточного времени
        // Однако onError надо выполнить
        long timeout = promiseTask.equals(promiseTask.getPromise().getOnError())
                ? 6_000L
                : promiseTask.getPromise().getRemainingMs();

        brokerMemoryConfiguration.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, timeout));
        addIdle();
        serviceBell();
    }

    public ExpirationMsImmutableEnvelope<AbstractPromiseTask> getPromiseTask() {
        return brokerMemoryConfiguration.get().poll();
    }

    @Override
    public void helper() {
        // Если потоки остановились из-за RateLimit или задачи закончились, их надо пошевелить немного, но только в том
        // случае, если парк не пустой
        if (!isParkQueueEmpty()) {
            serviceBell();
        }
    }

}
