package ru.jamsys.core.resource.thread;

import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.extension.Closable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ThreadPool extends AbstractPool<ThreadEnvelope> implements Closable {

    AtomicInteger counter = new AtomicInteger(1);

    final private Function<ThreadEnvelope, Boolean> consumer;

    public ThreadPool(
            String name,
            int min,
            Function<ThreadEnvelope, Boolean> consumer
    ) {
        super(name, min, ThreadEnvelope.class);
        this.consumer = consumer;
    }

    @Override
    public ThreadEnvelope createPoolItem() {
        return new ThreadEnvelope(getName() + "-" + counter.getAndIncrement(), this, consumer);
    }

    @Override
    public void closePoolItem(ThreadEnvelope threadEnvelope) {
        if (threadEnvelope.isInit()) {
            threadEnvelope.shutdown();
        }
    }

    @Override
    public boolean checkExceptionOnComplete(Exception e) {
        return false;
    }

    public void wakeUp() {
        if (!isRun.get()) {
            return;
        }
        // Первичный замысел был, что управлением должен заниматься отдельный поток
        // Но бывает такое, что у пулов может быть min = 0
        // Pool keepAlive, тоже не запускается из-за того что при инициализации min = 0) - контролировать некому!)
        if (isEmpty()) {
            addPoolItemIfEmpty();
        }
        ThreadEnvelope threadEnvelope = getPoolItem();
        if (threadEnvelope != null) {
            threadEnvelope.run();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

}
