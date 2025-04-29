package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.component.manager.item.BrokerMemoryImpl;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.rate.limit.RateLimitFactory;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

public class PoolThreadPromiseTask
        extends AbstractPoolPrivate<Void, Void, ThreadResourcePromiseTask>
        implements ManagerElement {

    AtomicInteger counter = new AtomicInteger(1);

    @Getter
    private final Manager.Configuration<RateLimitItem> rateLimitConfiguration;

    @SuppressWarnings("all")
    private final Manager.Configuration<BrokerMemory> brokerMemoryConfiguration;

    public PoolThreadPromiseTask(String key) {
        super(key);
        rateLimitConfiguration = App.get(Manager.class).configure(
                RateLimitItem.class,
                key,
                RateLimitFactory.TPS::create
        );

        brokerMemoryConfiguration = App.get(Manager.class).configure(
                BrokerMemory.class,
                key,
                key1 -> new BrokerMemoryImpl<AbstractPromiseTask>(
                        key1,
                        App.context,
                        promiseTask -> promiseTask.getPromise().setError(
                                "::drop",
                                new RuntimeException(App.getUniqueClassName(PoolThreadPromiseTask.class))
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    public void addPromiseTask(AbstractPromiseTask promiseTask) {
        // Контроль TPS управляется в реализации самого потока, допустим вставим 1000 элементов
        // окей, просто что-то умрёт в брокере, но вставку как будто не надо ограничивать
        // rateLimit.checkOrThrow();
        long timeout = promiseTask.getPromise().getExpiryRemainingMs();
        brokerMemoryConfiguration.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, timeout));
        isAvailablePoolItem();
        serviceBell();
    }

    @SuppressWarnings("unchecked")
    public ExpirationMsImmutableEnvelope<AbstractPromiseTask> getPromiseTask() {
        return brokerMemoryConfiguration.get().pollLast();
    }

    @Override
    public ThreadResourcePromiseTask createPoolItem() {
        ThreadResourcePromiseTask threadResourcePromiseTask = new ThreadResourcePromiseTask(
                getKey(),
                counter.getAndIncrement(),
                this
        );
        threadResourcePromiseTask.run();
        return threadResourcePromiseTask;
    }

    @Override
    public void closePoolItem(ThreadResourcePromiseTask poolItem) {
        poolItem.shutdown();
    }

    @Override
    public boolean checkFatalException(ThreadResourcePromiseTask poolItem, Throwable th) {
        return false;
    }


}
