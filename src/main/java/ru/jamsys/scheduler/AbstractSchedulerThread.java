package ru.jamsys.scheduler;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.Procedure;
import ru.jamsys.thread.CustomThreadFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class AbstractSchedulerThread implements SchedulerThread {

    @Setter
    protected boolean debug = false;

    private final ScheduledExecutorService executor;
    private final AtomicBoolean isRun = new AtomicBoolean(false);

    @Getter
    private final String name;
    @Getter
    private final long periodMillis;

    public AbstractSchedulerThread(String name, long periodMillis) {
        this.name = name;
        this.periodMillis = periodMillis;
        executor = Executors.newScheduledThreadPool(1, new CustomThreadFactory(getName()));
    }

    public void run() {
        try {
            run(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> void run(T t) {
        try {
            if (isRun.compareAndSet(false, true)) {
                executor.scheduleAtFixedRate(() -> {
                    try {
                        getConsumer().accept(t);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 1, getPeriodMillis(), TimeUnit.MILLISECONDS);
            } else {
                new Exception("Scheduler " + name + " already running").printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if (isRun.get()) { //Выключать будем только то, что включено)
            executor.shutdownNow();
            isRun.set(false);
        }
    }

    @Override
    public boolean isActive() {
        return  isRun.get();
    }

}
