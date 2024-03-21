package ru.jamsys.thread;

import ru.jamsys.App;
import ru.jamsys.component.RateLimit;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.pool.AbstractPool;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;
import ru.jamsys.util.UtilJson;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class ThreadPool extends AbstractPool<ThreadEnvelope> implements RunnableInterface {

    AtomicInteger counter = new AtomicInteger(1);

    final private BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer;

    final private RateLimitItem rateLimitItemThread;

    public ThreadPool(
            String name,
            int min,
            int initMax,
            BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer
    ) {
        super(name, min, initMax);
        this.rateLimitItemThread = App.context.getBean(RateLimit.class).get(getClass() + ".Thread." + name);
        this.consumer = consumer;
    }

    @Override
    public ThreadEnvelope createResource() {
        return new ThreadEnvelope(getName() + "-" + counter.getAndIncrement(), this, rateLimitItemPool, consumer);
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
        if (!isRun.get()) {
            return;
        }
        // Первичный замысел был, что управлением должен заниматься отдельный поток
        // Но бывает такое, что у пулов может быть min = 0
        // Pool keepAlive, тоже не запускается из-за того что при инициализации min = 0) - контролировать некому!)
        if (isEmpty()) {
            keepAlive();
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

    public void testRun() {
        new Thread(() -> {
            while (true) {
                keepAlive();
                Util.sleepMs(3000);
            }
        }).start();
        new Thread(() -> {
            while (true) {
                List<Statistic> statistics = flushAndGetStatistic(new HashMap<>(), new HashMap<>(), null);
                System.out.println(UtilJson.toString(statistics, "{}"));
                Util.sleepMs(1000);
            }
        }).start();
    }
}
