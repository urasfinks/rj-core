package ru.jamsys.thread;

import ru.jamsys.App;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.pool.Pool;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public class ThreadEnvelope {

    private final Thread thread;
    private final AtomicBoolean isWhile = new AtomicBoolean(true);
    private final AtomicBoolean isRun = new AtomicBoolean(false);
    private final AtomicBoolean inPark = new AtomicBoolean(false);
    private final Pool<ThreadEnvelope> pool;

    public ThreadEnvelope(String name, Pool<ThreadEnvelope> pool, Function<AtomicBoolean, Boolean> consumer) {
        this.pool = pool;
        thread = new Thread(() -> {
            Thread curThread = Thread.currentThread();
            while (isWhile.get() && !curThread.isInterrupted()) {
                boolean isContinue = false;
                try {
                    isContinue = consumer.apply(isWhile);
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
                if (!isContinue) {
                    pause();
                }
            }
            isRun.set(false);
        });
        thread.setName(name);
    }

    private void pause() {
        if (isRun.get() && inPark.compareAndSet(false, true)) {
            pool.complete(this, null);
            LockSupport.park(thread);
        }
    }

    public void resume() {
        if (isRun.get() && inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
        }
    }

    public void run() {
        //Что бы второй раз не получилось запустить поток после остановки проверим на isWhile
        if (isWhile.get() && isRun.compareAndSet(false, true)) {
            thread.start(); //start() - create new thread / run() - Runnable run in main thread
        }
    }

    synchronized public void shutdown() {
        //ЧТо бы что-то тушит, надо что бы это что-то было поднято)
        if (isRun.get() && isWhile.get()) {
            isWhile.set(false); //Говорим закончить
            resume(); //Выводим из возможного паркинга
            long timeOutMs = 1000;
            long startTimeMs = System.currentTimeMillis();
            while (isRun.get()) { //Пытаемся подождать пока потоки самостоятельно закончат свою работу
                Util.sleepMs(timeOutMs / 4);
                if (System.currentTimeMillis() - startTimeMs > timeOutMs) { //Не смогли за отведённое время
                    new RuntimeException(getClass().getSimpleName() + " Self-stop timeOut shutdown " + timeOutMs + "ms").printStackTrace();
                    //App.context.getBean(ExceptionHandler.class).handler(new RuntimeException(getClass().getSimpleName() + " Self-stop timeOut shutdown " + timeOutMs + "ms"));
                    break;
                }
            }
            if (!isRun.get()) {
                return;
            } else {
                Util.logConsole("Thread " + thread.getName() + " > interrupt()");
                try {
                    thread.interrupt();
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
            }
            startTimeMs = System.currentTimeMillis();
            while (isRun.get()) { //Пытаемся подождать пока потоки выйдут от interrupt
                Util.sleepMs(timeOutMs / 4);
                if (System.currentTimeMillis() - startTimeMs > timeOutMs) { //Не смогли за отведённое время
                    new RuntimeException(getClass().getSimpleName() + " interrupt timeOut shutdown " + timeOutMs + " ms").printStackTrace();
                    break;
                }
            }
            if (!isRun.get()) {
                return;
            } else {
                Util.logConsole("Thread " + thread.getName() + " > stop()");
                try {
                    thread.stop(); //Ну как бы всё, извините, на этом мои полномочия всё
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
                clearOnStopThread(thread);
            }
            isRun.set(false);
        }
    }

    public static void clearOnStopThread(Thread thread) {
        Broker broker = App.context.getBean(Broker.class);
        Queue<TaskStatistic> taskHandlerStatisticQueue = broker.get(TaskStatistic.class.getSimpleName());
        Util.riskModifierCollection(
                null,
                taskHandlerStatisticQueue.getCloneQueue(null),
                new TaskStatistic[0],
                (TaskStatistic taskStatisticExecute) -> {
                    if (taskStatisticExecute.getThread().equals(thread)) {
                        taskHandlerStatisticQueue.remove(taskStatisticExecute);
                    }
                });
    }

}
