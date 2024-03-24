package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.RunnableComponent;
import ru.jamsys.template.cron.CronTask;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.pool.ThreadPool;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class GeneratorManager implements RunnableComponent {

    final private ThreadPool threadPool;

    public GeneratorManager(Dictionary dictionary, ExceptionHandler exceptionHandler, Broker broker, TaskManager taskManager, RateLimit rateLimit) {
        this.threadPool = new ThreadPool(
                getClass().getSimpleName(),
                1,
                1,
                (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                    long nextStartMs = System.currentTimeMillis();
                    while (isWhile.get() && threadEnvelope.isNotInterrupted()) {
                        nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);
                        long curTimeMs = System.currentTimeMillis();
                        for (CronTask cronTask : dictionary.getListCronTask()) {
                            if (cronTask.getCron().isTimeHasCome(curTimeMs)) {
                                try {
                                    taskManager.addTask(cronTask.getTask());
                                } catch (Exception e) {
                                    exceptionHandler.handler(e);
                                }
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
                    Util.logConsole("STOP");
                    return false;
                }
        );
    }

    @Override
    public void run() {
        threadPool.run();
        threadPool.wakeUp();
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
