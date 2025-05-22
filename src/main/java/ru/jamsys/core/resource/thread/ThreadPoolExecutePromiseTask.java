package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitTps;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ThreadPoolExecutePromiseTask
        extends AbstractPoolPrivate<Void, Void, ThreadExecutePromiseTask>
        implements ManagerElement, CascadeKey {

    AtomicInteger counter = new AtomicInteger(1);

    private final Manager.Configuration<RateLimit> rateLimitConfiguration;

    @SuppressWarnings("all")
    private final Manager.Configuration<BrokerMemory> brokerMemoryConfiguration;

    public ThreadPoolExecutePromiseTask(String ns) {
        super(ns);
        rateLimitConfiguration = RateLimitTps.getInstanceConfigure(getCascadeKey(ns));
        brokerMemoryConfiguration = App.get(Manager.class).configure(
                BrokerMemory.class,
                getCascadeKey(ns),
                ns1 -> new BrokerMemory<AbstractPromiseTask>(
                        ns1,
                        App.context,
                        promiseTask -> promiseTask.getPromise().setError(
                                "::drop",
                                new RuntimeException(App.getUniqueClassName(ThreadPoolExecutePromiseTask.class))
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    public void addPromiseTask(AbstractPromiseTask promiseTask) {
        // Контроль TPS управляется в реализации самого потока, тут ограничивать ничего не надо.
        // Есть задача onError, которая может вызываться после вызова timeOut. У обещания больше нет остаточного времени
        // Однако onError надо выполнить
        long timeout = promiseTask.equals(promiseTask.getPromise().getOnError())
                ? 6_000L
                : promiseTask.getPromise().getExpiryRemainingMs();

        brokerMemoryConfiguration.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, timeout));
        isAvailablePoolItem();
        serviceBell();
    }

    @SuppressWarnings("unchecked")
    public ExpirationMsImmutableEnvelope<AbstractPromiseTask> getPromiseTask() {
        return brokerMemoryConfiguration.get().pollLast();
    }

    @Override
    public ThreadExecutePromiseTask createPoolItem() {
        ThreadExecutePromiseTask threadExecutePromiseTask = new ThreadExecutePromiseTask(
                getCascadeKey(ns),
                counter.getAndIncrement(),
                this,
                rateLimitConfiguration
        );
        threadExecutePromiseTask.run();
        return threadExecutePromiseTask;
    }

    @Override
    public void closePoolItem(ThreadExecutePromiseTask poolItem) {
        poolItem.shutdown();
    }

    @Override
    public void helper() {
        // Если потоки остановились из-за RateLimit или задачи закончились, их надо пошевелить немного
        serviceBell();
    }

}
