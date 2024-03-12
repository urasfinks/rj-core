package ru.jamsys.thread;

import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.pool.AbstractPool;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ThreadPool extends AbstractPool<ThreadEnvelope> implements RunnableComponent {

    AtomicInteger index = new AtomicInteger(1);

    final private Function<AtomicBoolean, Boolean> consumer;

    public ThreadPool(String name, int min, int max, long keepAliveMs, Function<AtomicBoolean, Boolean> consumer) {
        super(name, min, max, keepAliveMs);
        this.consumer = consumer;
    }

    @Override
    public ThreadEnvelope createResource() {
        ThreadEnvelope threadEnvelope = new ThreadEnvelope(getName() + "-" + index.getAndIncrement(), this, consumer);
        threadEnvelope.run();
        return threadEnvelope;
    }

    @Override
    public void closeResource(ThreadEnvelope resource) {
        resource.shutdown();
    }

    @Override
    public boolean checkExceptionOnComplete(Exception e) {
        return false;
    }

    @SuppressWarnings("unused")
    public void wakeUp() {
        if (isAllInPark()) {
            try {
                ThreadEnvelope resource = getResource(null);
                resource.resume();
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }

    }

    @Override
    public void reload() {
        shutdown();
        run();
    }
}
