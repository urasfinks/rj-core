package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.component.manager.item.BrokerMemoryImpl;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.rate.limit.RateLimitFactory;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

public class PoolThreadPromiseTask
        extends AbstractPoolPrivate<Void, Void, ThreadResourcePromiseTask>
        implements ManagerElement, CascadeKey {

    AtomicInteger counter = new AtomicInteger(1);

    @Getter
    private final Manager.Configuration<RateLimitItem> rateLimitConfiguration;

    @SuppressWarnings("all")
    @Getter
    private final Manager.Configuration<BrokerMemory> brokerPromiseTaskConfiguration;

    public PoolThreadPromiseTask(String ns) {
        super(ns);
        rateLimitConfiguration = App.get(Manager.class).configure(
                RateLimitItem.class,
                getCascadeKey(ns),
                RateLimitFactory.TPS::create
        );

        brokerPromiseTaskConfiguration = App.get(Manager.class).configure(
                BrokerMemory.class,
                getCascadeKey(ns),
                ns1 -> new BrokerMemoryImpl<AbstractPromiseTask>(
                        ns1,
                        App.context,
                        promiseTask -> {
                            System.out.println("::DROP::" + promiseTask.getNs());
                            promiseTask.getPromise().setError(
                                    "::drop",
                                    new RuntimeException(App.getUniqueClassName(PoolThreadPromiseTask.class))
                            );
                        }
                )
        );
    }

    @SuppressWarnings("unchecked")
    public void addPromiseTask(AbstractPromiseTask promiseTask) {
        // Контроль TPS управляется в реализации самого потока, тут ограничивать ничего не надо
        // Есть задача onError, которая может вызываться после вызова timeOut. У обещания больше нет остаточного времени
        // Однако onError надо выполнить
        long timeout = promiseTask.equals(promiseTask.getPromise().getOnError())
                ? 6_000L
                : promiseTask.getPromise().getExpiryRemainingMs();

        brokerPromiseTaskConfiguration.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, timeout));
        isAvailablePoolItem();
        serviceBell();
    }

    @SuppressWarnings("unchecked")
    public ExpirationMsImmutableEnvelope<AbstractPromiseTask> getPromiseTask() {
        return brokerPromiseTaskConfiguration.get().pollLast();
    }

    @Override
    public ThreadResourcePromiseTask createPoolItem() {
        ThreadResourcePromiseTask threadResourcePromiseTask = new ThreadResourcePromiseTask(
                getCascadeKey(ns),
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

    @Override
    public void helper() {
        // Если потоки остановились из-за RateLimit или задачи закончились, их надо пошевелить немного
        serviceBell();
    }

}
