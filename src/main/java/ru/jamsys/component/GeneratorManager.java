package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.RunnableComponent;
import ru.jamsys.task.Task;
import ru.jamsys.task.handler.TaskHandler;
import ru.jamsys.template.cron.CronTask;
import ru.jamsys.thread.ThreadPool;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class GeneratorManager extends RunnableComponent {

    final private ThreadPool threadPool;

    public GeneratorManager(Dictionary dictionary, ExceptionHandler exceptionHandler) {
        this.threadPool = new ThreadPool(getClass().getSimpleName(), 1, 1, 60000, (AtomicBoolean isWhile) -> {
            Thread currentThread = Thread.currentThread();
            long nextStartMs = System.currentTimeMillis();
            while (isWhile.get() && !currentThread.isInterrupted()) {
                nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);
                long curTimeMs = System.currentTimeMillis();
                for (CronTask cronTask : dictionary.getListCronTask()) {
                    if (cronTask.getCron().getNext(curTimeMs) <= curTimeMs) {
                        Task task = cronTask.getTask();
                        //TODO: переделать, что бы эта таска улетала в общий пул обработки, а не исполнялась тут
                        @SuppressWarnings("unchecked")
                        TaskHandler<Task> taskHandler = dictionary.getTaskHandler().get(task.getClass());
                        if (taskHandler != null) {
                            try {
                                taskHandler.run(task, isWhile);
                            } catch (Exception e) {
                                exceptionHandler.handler(e);
                            }
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
