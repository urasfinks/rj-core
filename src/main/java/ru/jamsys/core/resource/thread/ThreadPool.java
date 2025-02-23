package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.ManagerRateLimit;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitFactory;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool extends AbstractPoolPrivate<Void, Void, ThreadResource> implements ClassEquals {

    AtomicInteger counter = new AtomicInteger(1);

    private final Broker<PromiseTask> broker;

    @Getter
    private final RateLimit rateLimit;

    public ThreadPool(CascadeName parentCascadeName, String key) {
        super(parentCascadeName, key);
        rateLimit = App.get(ManagerRateLimit.class).get(getCascadeName());
        rateLimit.computeIfAbsent("tps", RateLimitFactory.TPS);
        this.broker = App.get(ManagerBroker.class)
                .initAndGet(getCascadeName(), PromiseTask.class, promiseTask ->
                        promiseTask.
                                getPromise().
                                setError(new RuntimeException(
                                        App.getUniqueClassName(ThreadPool.class) + ".broker->drop(task)")
                                )
                );
    }

    public void addPromiseTask(PromiseTask promiseTask) {
        // Контроль TPS управляется в реализации самого потока, допустим вставим 1000 элементов
        // окей, просто что-то умрёт в брокере, но вставку как будто не надо ограничивать, но вот допустим
        //rateLimit.checkOrThrow();
        long timeout = promiseTask.isTerminated() ? 6_000L : promiseTask.getPromise().getExpiryRemainingMs();
        broker.add(new ExpirationMsImmutableEnvelope<>(promiseTask, timeout));
        isAvailablePoolItem();
        serviceBell();
    }

    public ExpirationMsImmutableEnvelope<PromiseTask> getPromiseTask() {
        return broker.pollLast();
    }

    @Override
    public ThreadResource createPoolItem() {
        return new ThreadResource(this, counter.getAndIncrement() + "", this);
    }

    @Override
    public void closePoolItem(ThreadResource poolItem) {
        poolItem.shutdown();
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return true;
    }

}
