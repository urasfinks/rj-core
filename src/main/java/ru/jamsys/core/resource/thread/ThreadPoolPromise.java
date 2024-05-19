package ru.jamsys.core.resource.thread;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.pool.AbstractPoolPrivate;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolPromise extends AbstractPoolPrivate<Void, Void, ThreadEnvelopePromise> {

    AtomicInteger counter = new AtomicInteger(1);

    private final BrokerManager<PromiseTask> brokerManager;

    @SuppressWarnings("all")
    public ThreadPoolPromise(String name, int min) {
        super(name, min, ThreadEnvelopePromise.class);
        brokerManager = App.context.getBean(BrokerManager.class);
    }

    public void addPromiseTask(PromiseTask promiseTask) throws Exception {
        brokerManager.add(name, new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getPromise().getExpiryRemainingMs()));
        addIfPoolEmpty();
        serviceBell();
    }

    public ExpirationMsImmutableEnvelope<PromiseTask> getPromiseTask() {
        return brokerManager.pollLast(name);
    }

    @Override
    public ThreadEnvelopePromise createPoolItem() {
        return new ThreadEnvelopePromise(getName() + "-" + counter.getAndIncrement(), this);
    }

    @Override
    public void closePoolItem(ThreadEnvelopePromise poolItem) {
        poolItem.shutdown();
    }

    @Override
    public boolean checkExceptionOnComplete(Exception e) {
        return false;
    }

}
