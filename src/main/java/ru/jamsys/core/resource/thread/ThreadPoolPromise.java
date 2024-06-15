package ru.jamsys.core.resource.thread;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolPromise extends AbstractPoolPrivate<Void, Void, ThreadResource> {

    AtomicInteger counter = new AtomicInteger(1);

    private final Broker<PromiseTask> brokerManagerElement;

    public ThreadPoolPromise(String name) {
        super(name, ThreadResource.class);
        this.brokerManagerElement = App.get(ManagerBroker.class).initAndGet(getName(), PromiseTask.class, null);
    }

    public void addPromiseTask(PromiseTask promiseTask) {
        brokerManagerElement.add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getPromise().getExpiryRemainingMs()));
        addIfPoolEmpty();
        serviceBell();
    }

    public ExpirationMsImmutableEnvelope<PromiseTask> getPromiseTask() {
        return brokerManagerElement.pollLast();
    }

    @Override
    public ThreadResource createPoolItem() {
        return new ThreadResource(getName() + "-" + counter.getAndIncrement(), this);
    }

    @Override
    public void closePoolItem(ThreadResource poolItem) {
        poolItem.shutdown();
    }

    @Override
    public boolean checkCriticalOfExceptionOnComplete(Exception e) {
        return false;
    }

}
