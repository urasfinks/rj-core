package ru.jamsys.thread;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.App;
import ru.jamsys.component.Broker;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.task.TaskHandler;
import ru.jamsys.task.TaskHandlerStatistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Getter
@Setter
public class ExecutorServiceScheduler extends AbstractExecutorService {

    private java.util.concurrent.ExecutorService executorService;
    private final long delayMs;

    private List<TaskHandler> listTaskHandler = new ArrayList<>();
    private Broker broker;

    public ExecutorServiceScheduler(long delayMs) {
        this.delayMs = delayMs;
        broker = App.context.getBean(Broker.class);
    }

    @Override
    public boolean run() {
        if (super.run()) {
            executorService = Executors.newSingleThreadExecutor(
                    new CustomThreadFactory(getClass().getSimpleName() + "-" + delayMs)
            );
            executorService.submit(() -> {
                isWhile.set(true);
                Thread currentThread = Thread.currentThread();
                listThread.add(currentThread);
                long nextStartMs = System.currentTimeMillis();
                while (isWhile.get() && !currentThread.isInterrupted()) {
                    nextStartMs = Util.zeroLastNDigits(nextStartMs + delayMs, 3);
                    listTaskHandler.forEach(taskHandler -> {
                        if (isWhile.get()) {
                            TaskHandlerStatistic taskHandlerStatistic = new TaskHandlerStatistic(currentThread, null, taskHandler);
                            try {
                                broker.add(TaskHandlerStatistic.class, taskHandlerStatistic);
                            } catch (Exception e) {
                                App.context.getBean(ExceptionHandler.class).handler(e);
                            }
                            try {
                                taskHandler.run(null, isWhile);
                            } catch (Exception e) {
                                App.context.getBean(ExceptionHandler.class).handler(e);
                            }
                            taskHandlerStatistic.finish();
                        }
                    });
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
                listThread.remove(currentThread);
                Util.logConsole(currentThread.getName() + ": STOP");
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean shutdown() {
        if (super.shutdown()) {
            executorService.shutdown();
            return true;
        }
        return false;
    }

}
