package ru.jamsys.thread;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.component.TaskManager;
import ru.jamsys.extension.AbstractPoolItem;
import ru.jamsys.pool.AbstractPool;
import ru.jamsys.pool.Pool;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;

/*
 * После остановки поток нельзя переиспользовать
 * Нельзя будет его заново запустить - надо полностью пересоздавать объект
 * */

@ToString(onlyExplicitlyIncluded = true)
public class ThreadEnvelope extends AbstractPoolItem {

    private final Thread thread;

    @Getter
    private final String name;

    @ToString.Include
    private final Pool<ThreadEnvelope> pool;

    private final AtomicBoolean isInit = new AtomicBoolean(false);

    private final AtomicBoolean isRun = new AtomicBoolean(false);

    private final AtomicBoolean isWhile = new AtomicBoolean(true);

    private final AtomicBoolean inPark = new AtomicBoolean(false);

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private final StringBuilder info = new StringBuilder();

    @Setter
    @Getter
    private int maxCountIteration = 100; //Защита от бесконечных задач

    private final AtomicInteger countOperation = new AtomicInteger(0);

    public boolean isInit() {
        return isInit.get();
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("isInit: ").append(isInit.get()).append("; ");
        sb.append("isRun: ").append(isRun.get()).append("; ");
        sb.append("isWhile: ").append(isWhile.get()).append("; ");
        sb.append("inPark: ").append(inPark.get()).append("; ");
        sb.append("isShutdown: ").append(isShutdown.get()).append("; ");
        sb.append("countOperation: ").append(countOperation.get()).append("; ");
        return sb.toString();
    }

    public boolean isNotInterrupted() {
        return !thread.isInterrupted();
    }

    public boolean isOverflowIteration() {
        // countOperation - защита от бесконечных задач
        // Предположим, что поменялось максимальное кол-во потоков и надо срезать потоки
        return countOperation.getAndIncrement() >= maxCountIteration;
    }

    public ThreadEnvelope(String name, Pool<ThreadEnvelope> pool, RateLimitItem rateLimitItem, BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer) {
        info
                .append("[")
                .append(Util.msToDataFormat(System.currentTimeMillis()))
                .append("] ")
                .append("create: ")
                .append(Thread.currentThread().getName())
                .append(" with name: ")
                .append(name)
                .append("\r\n");
        this.name = name;
        this.pool = pool;
        thread = new Thread(() -> {
            AbstractPool.contextPool.set(pool);
            while (isWhile.get() && isNotInterrupted()) {
                active();
                if (rateLimitItem.isOverflowTps()) {
                    pause(true);
                    continue;
                }
                if (isOverflowIteration()) {
                    pause(false);
                    continue;
                }
                try {
                    if (consumer.apply(isWhile, this)) {
                        continue;
                    }
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
                //Конце итерации цикла всегда pause()
                pause(true);
            }
            isRun.set(false);
        });
        thread.setName(name);
    }

    private void raiseUp(String cause, String action) {
        App.context.getBean(ExceptionHandler.class).handler(
                new RuntimeException("class: " + getClass().getSimpleName()
                        + "; action: " + action
                        + "; cause: " + cause + " \r\n"
                        + info)
        );
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean pause(boolean isFinish) {
        if (!isInit.get()) {
            raiseUp("Thread not initialize", "pause()");
            return false;
        }
        if (inPark.compareAndSet(false, true)) {
            if (isShutdown.get()) {
                pool.removeForce(this, isFinish);
            } else {
                pool.complete(this, null, isFinish);
                LockSupport.park(thread);
                return true;
            }
        }
        return false;
    }

    public boolean run() {
        if (isShutdown.get()) {
            raiseUp("Thread shutdown", "run()");
            return false;
        }
        //Что бы второй раз не получилось запустить поток после остановки проверим на isWhile
        if (isInit.compareAndSet(false, true)) {
            if (isRun.compareAndSet(false, true)) {
                info
                        .append("[")
                        .append(Util.msToDataFormat(System.currentTimeMillis()))
                        .append("]")
                        .append(" run: ")
                        .append(Thread.currentThread().getName())
                        .append(";\r\n");
                thread.start(); //start() - create new thread / run() - Runnable run in main thread
                return true;
            } else {
                raiseUp("WTF?", "run()");
            }
        } else if (inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
            return true;
        }
        return false;
    }

    synchronized public boolean shutdown() {
        if (!isInit.get()) {
            raiseUp("Thread not initialize", "shutdown()");
            return false;
        }
        if (isShutdown.compareAndSet(false, true)) { //Что бы больше никто не смог начать останавливать
            doShutdown();
            return true;
        } else {
            raiseUp("Thread shutdown", "shutdown()");
        }
        return false;
    }

    private void doShutdown() {
        info
                .append("[")
                .append(Util.msToDataFormat(System.currentTimeMillis()))
                .append("] ")
                .append("shutdown: ")
                .append(Thread.currentThread().getName())
                .append("\r\n");
        //#1
        isWhile.set(false); //Говорим закончить
        //#2
        if (inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
        }
        //Выводим из возможного паркинга
        //#3
        isShutdown.set(true);

        long timeOutMs = 1000;
        long startTimeMs = System.currentTimeMillis();
        while (isRun.get()) { //Пытаемся подождать пока потоки самостоятельно закончат свою работу
            Util.sleepMs(timeOutMs / 4);
            if (System.currentTimeMillis() - startTimeMs > timeOutMs) { //Не смогли за отведённое время
                raiseUp("timeOut. The thread is keep alive", "isWhile.set(false)");
                break;
            }
        }
        if (!isRun.get()) {
            return;
        } else {
            Util.logConsole("do Thread " + thread.getName() + ".interrupt()", true);
            thread.interrupt();
        }
        startTimeMs = System.currentTimeMillis();
        while (isRun.get()) { //Пытаемся подождать пока потоки выйдут от interrupt
            Util.sleepMs(timeOutMs / 4);
            if (System.currentTimeMillis() - startTimeMs > timeOutMs) { //Не смогли за отведённое время
                raiseUp("timeOut. The thread is keep alive", "interrupt()");
                break;
            }
        }
        if (!isRun.get()) {
            return;
        } else {
            Util.logConsole("do Thread " + thread.getName() + ".stop()", true);
            try {
                thread.stop(); //Ну как бы всё, извините, на этом мои полномочия всё
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
            App.context.getBean(TaskManager.class).removeInQueueStatistic(this);
        }
        pool.removeForce(this, true);
        isRun.set(false);
    }

    @Override
    public void polled() {
        countOperation.set(0);
    }
}
