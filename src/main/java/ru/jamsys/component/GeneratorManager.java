package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.RunnableComponent;
import ru.jamsys.template.cron.CronTask;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.ThreadPool;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class GeneratorManager implements RunnableComponent {

    final private ThreadPool threadPool;

    final private TaskManager taskManager;

    public GeneratorManager(Dictionary dictionary, ExceptionHandler exceptionHandler, Broker broker, TaskManager taskManager) {
        this.taskManager = taskManager;
        this.threadPool = new ThreadPool(
                getClass().getSimpleName(),
                1,
                1,
                60000,
                (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                    Thread currentThread = Thread.currentThread();
                    long nextStartMs = System.currentTimeMillis();
                    while (isWhile.get() && !currentThread.isInterrupted()) {
                        nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);
                        long curTimeMs = System.currentTimeMillis();
                        for (CronTask cronTask : dictionary.getListCronTask()) {
                            if (cronTask.getCron().getNext(curTimeMs) <= curTimeMs) {
                                taskManager.add(cronTask.getTask());
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
                }
        );
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
