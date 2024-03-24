package ru.jamsys.pool;

import lombok.Getter;
import ru.jamsys.App;
import ru.jamsys.component.RateLimit;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.statistic.RateLimitGroup;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.thread.ThreadEnvelope;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class ThreadPool extends AbstractPool<ThreadEnvelope> implements RunnableInterface {

    AtomicInteger counter = new AtomicInteger(1);

    final private BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer;

    @Getter
    final private RateLimitItem rateLimitItemThread;

    public ThreadPool(
            String name,
            int min,
            int initMax,
            BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer
    ) {
        super(name, min, initMax);
        this.rateLimitItemThread = App.context.getBean(RateLimit.class).get(RateLimitGroup.THREAD, getClass(), name);
        this.consumer = consumer;
    }

    @Override
    public ThreadEnvelope createResource() {
        return new ThreadEnvelope(getName() + "-" + counter.getAndIncrement(), this, rateLimitItemThread, consumer);
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
    public void run() {
        super.run();
        rateLimitItemThread.setActive(true);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        rateLimitItemThread.setActive(false);
    }

}
