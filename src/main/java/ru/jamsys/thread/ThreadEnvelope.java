package ru.jamsys.thread;

import lombok.Getter;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.task.TaskHandlerStatistic;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;

public class ThreadEnvelope {

    private final Thread thread;
    private final AtomicBoolean isWhile = new AtomicBoolean(true);
    private final AtomicBoolean isRun = new AtomicBoolean(false);
    private final AtomicBoolean inPark = new AtomicBoolean(false);

    @Getter
    long lastExecute = 0;

    public ThreadEnvelope(BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer) {
        thread = new Thread(() -> {
            Thread curThread = Thread.currentThread();
            while (isWhile.get() && !curThread.isInterrupted()) {
                lastExecute = System.currentTimeMillis();
                if (!consumer.apply(isWhile, this)) {
                    pause();
                }
            }
            isRun.set(false);
        });
        thread.setName("AnyKey");
    }

    private void pause() {
        if (isRun.get() && inPark.compareAndSet(false, true)) {
            LockSupport.park(thread);
        }
    }

    public void resume() {
        if (isRun.get() && inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
        }
    }

    public boolean run() {
        //Что бы второй раз не получилось запустить поток после остановки проверим на isWhile
        if (isWhile.get() && isRun.compareAndSet(false, true)) {
            thread.start();
            return true;
        }
        return false;
    }

    public boolean shutdown() {
        //ЧТо бы что-то тушит, надо что бы это что-то было поднято)
        if (isRun.get()) {
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
                return true;
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
                return true;
            } else {
                Util.logConsole("Thread " + thread.getName() + " > stop()");
                try {
                    thread.stop(); //Ну как бы всё, извините, на этом мои полномочия всё
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
                TaskHandlerStatistic.clearOnStopThread(thread);
            }
            isRun.set(false);
            return true;
        }
        return false;
    }
}
