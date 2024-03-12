package ru.jamsys.component;

import lombok.Getter;
import org.springframework.stereotype.Component;
import ru.jamsys.RunnableComponent;
import ru.jamsys.task.instance.StatisticTask;
import ru.jamsys.template.cron.Cron;
import ru.jamsys.template.cron.CronTask;
import ru.jamsys.RunnableInterface;
import ru.jamsys.thread.ThreadPool;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Generator extends RunnableComponent implements RunnableInterface {

    final private ThreadPool threadPool;

    @Getter
    private final List<CronTask> listCronTask = new ArrayList<>();

    public Generator() {
        listCronTask.add(new CronTask(new Cron("*/5 * * * * *"), new StatisticTask()));
        this.threadPool = new ThreadPool(getClass().getSimpleName(), 1, 1, 60000, (AtomicBoolean isWhile) -> {
            Thread currentThread = Thread.currentThread();
            long nextStartMs = System.currentTimeMillis();
            while (isWhile.get() && !currentThread.isInterrupted()) {
                nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);
                long curTimeMs = System.currentTimeMillis();
                for (CronTask cronTask : listCronTask) {
                    if (cronTask.getCron().getNext(curTimeMs) <= curTimeMs) {
                        System.out.println(cronTask.getTask().getIndex());
                    }
                }
                if (isWhile.get()) {
                    long calcSleepMs = nextStartMs - System.currentTimeMillis();
                    if (calcSleepMs > 0) {
                        Util.sleepMs(calcSleepMs);
                    } else {
                        Util.sleepMs(1);//Что бы поймать Interrupt
                        nextStartMs = System.currentTimeMillis();
                    }
                } else {
                    break;
                }
            }
            Util.logConsole(currentThread.getName() + ": STOP");
            return false;
        });
    }

    @Override
    public void run() {
        threadPool.run();
    }

    @Override
    public void shutdown() {
        threadPool.shutdown();
    }

    @Override
    synchronized public void reload() {
        shutdown();
        run();
    }

}
