package ru.jamsys.thread;

import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.pool.AbstractPool;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;
import ru.jamsys.util.UtilJson;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class ThreadPool extends AbstractPool<ThreadEnvelope> implements RunnableInterface {

    @Override
    public boolean onCreateResourcePutInPark() {
        return false;
    }

    AtomicInteger index = new AtomicInteger(1);

    final private BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer;

    public ThreadPool(String name, int min, int max, long keepAliveMs, BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer) {
        super(name, min, max, keepAliveMs);
        this.consumer = consumer;
    }

    public boolean isEmpty() {
        return map.isEmpty();
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
            if (isEmpty()) {
                keepAlive();
            } else {
                try {
                    ThreadEnvelope threadEnvelope = getResource(null);
                    if (threadEnvelope != null) {
                        threadEnvelope.resume();
                    }
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
            }
        }
    }

    @Override
    public void reload() {
        shutdown();
        run();
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
