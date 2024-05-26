package ru.jamsys.core.resource.thread;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolPromise extends AbstractPoolPrivate<Void, Void, ThreadResource> {

    AtomicInteger counter = new AtomicInteger(1);

    private final ManagerElement<Broker<PromiseTask>, Void> brokerManagerElement;

    public ThreadPoolPromise(String name) {
        super(name, ThreadResource.class);
        this.brokerManagerElement = App.context.getBean(BrokerManager.class).get(getName(), PromiseTask.class);
    }

    public void addPromiseTask(PromiseTask promiseTask) {
        brokerManagerElement.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getPromise().getExpiryRemainingMs()));
        addIfPoolEmpty();
        serviceBell();
    }

    public ExpirationMsImmutableEnvelope<PromiseTask> getPromiseTask() {
        return brokerManagerElement.get().pollLast();
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
