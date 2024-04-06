package ru.jamsys.pool;

import ru.jamsys.extension.Closable;
import ru.jamsys.thread.ThreadEnvelope;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ThreadPool extends AbstractPool<ThreadEnvelope> implements Closable {

    AtomicInteger counter = new AtomicInteger(1);

    final private Function<ThreadEnvelope, Boolean> consumer;

    public ThreadPool(
            String name,
            int min,
            int initMax,
            Function<ThreadEnvelope, Boolean> consumer
    ) {
        super(name, min, initMax, ThreadEnvelope.class);
        this.consumer = consumer;
    }

    @Override
    public ThreadEnvelope createResource() {
        return new ThreadEnvelope(getName() + "-" + counter.getAndIncrement(), this, consumer);
    }

    @Override
    public void closeResource(ThreadEnvelope resource) {
        if (resource.isInit()) {
            resource.shutdown();
        }
    }

    @Override
    public boolean checkExceptionOnComplete(Exception e) {
        return false;
    }

    @SuppressWarnings("unused")
    public void wakeUp() {
        if (!isRun.get()) {
            return;
        }
        // Первичный замысел был, что управлением должен заниматься отдельный поток
        // Но бывает такое, что у пулов может быть min = 0
        // Pool keepAlive, тоже не запускается из-за того что при инициализации min = 0) - контролировать некому!)
        if (isEmpty()) {
            addResourceZeroPool();
        }
        ThreadEnvelope threadEnvelope = getResource();
        if (threadEnvelope != null) {
            threadEnvelope.run();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

}
