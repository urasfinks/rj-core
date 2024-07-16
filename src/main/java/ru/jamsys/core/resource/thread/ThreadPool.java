package ru.jamsys.core.resource.thread;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.ManagerRateLimit;
import ru.jamsys.core.component.manager.item.Broker;
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

    public ThreadPool(String index) {
        super(index);
        this.broker = App.get(ManagerBroker.class)
                .initAndGet(this.getIndex(), PromiseTask.class, promiseTask ->
                        promiseTask.
                                getPromise().
                                setErrorInRunTask(new RuntimeException(
                                        ThreadPool.class.getSimpleName() + ".broker->drop(task)")
                                )
                );
    }

    public void addPromiseTask(PromiseTask promiseTask) {
        long timeout = promiseTask.isTerminated() ? 6_000L : promiseTask.getPromise().getExpiryRemainingMs();
        broker.add(new ExpirationMsImmutableEnvelope<>(promiseTask, timeout));
        addIfPoolEmpty();
        serviceBell();
    }

    public ExpirationMsImmutableEnvelope<PromiseTask> getPromiseTask() {
        return broker.pollLast();
    }

    @Override
    public ThreadResource createPoolItem() {
        RateLimit rateLimit = App.get(ManagerRateLimit.class).get(getIndex());
        rateLimit.init(App.context, "tps", RateLimitFactory.TPS);
        return new ThreadResource(this.getIndex() + "-" + counter.getAndIncrement(), this);
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
