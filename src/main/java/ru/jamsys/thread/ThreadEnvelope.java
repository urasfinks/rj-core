package ru.jamsys.thread;

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

    @ToString.Include
    private final Pool<ThreadEnvelope> pool;

    private final AtomicBoolean isInit = new AtomicBoolean(false);

    private final AtomicBoolean isRun = new AtomicBoolean(false);

    private final AtomicBoolean isWhile = new AtomicBoolean(true);

    private final AtomicBoolean inPark = new AtomicBoolean(false);

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private final StringBuilder info = new StringBuilder();

    @Setter
    private int maxCountIteration = 100; //Защита от бесконечных задач

    private final AtomicInteger countOperation = new AtomicInteger(0);

    @SuppressWarnings("StringBufferReplaceableByString")
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("isInit: ").append(isInit.get()).append("; ");
        sb.append("isRun: ").append(isRun.get()).append("; ");
        sb.append("isWhile: ").append(isWhile.get()).append("; ");
        sb.append("inPark: ").append(inPark.get()).append("; ");
        sb.append("isShutdown: ").append(isShutdown.get()).append("; ");
        sb.append("maxCountIteration: ").append(maxCountIteration).append("; ");
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
        info.append("create: ").append(Thread.currentThread().getName()).append(" with name: ").append(name).append("; ");
        this.pool = pool;
        thread = new Thread(() -> {
            AbstractPool.contextPool.set(pool);
            while (isWhile.get() && isNotInterrupted()) {
                active();
                if (rateLimitItem.isOverflowTps() || isOverflowIteration()) {
                    pause();
                }
                try {
                    if (consumer.apply(isWhile, this)) {
                        continue;
                    }
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
                //Конце итерации цикла всегда pause()
                pause();
            }
            isRun.set(false);
        });
        thread.setName(name);
    }

    private void raiseUp(String status) {
        App.context.getBean(ExceptionHandler.class).handler(
                new RuntimeException(getClass().getSimpleName()
                        + " thread status [" + status + "]; info: "
                        + info)
        );
    }

    private void pause() {
        if (!isInit.get()) {
            raiseUp("NotInitialize");
            return;
        }
        if (inPark.compareAndSet(false, true)) {
            if (isShutdown.get()) {
                pool.removeForce(this);
            } else {
                pool.complete(this, null);
                LockSupport.park(thread);
            }
        }
    }

    public void run() {
        //Что бы второй раз не получилось запустить поток после остановки проверим на isWhile
        if (isInit.compareAndSet(false, true)) {
            if (isRun.compareAndSet(false, true)) {
                info.append("run: ").append(Thread.currentThread().getName()).append("; ");
                thread.start(); //start() - create new thread / run() - Runnable run in main thread
            } else {
                raiseUp("UNDEFINED STATUS WTF?");
            }
        } else if (inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
        }
    }

    synchronized public void shutdown() {
        if (!isInit.get()) {
            raiseUp("NotInitialize");
            return;
        }
        if (isShutdown.compareAndSet(false, true)) { //Что бы больше никто не смог начать останавливать
            doShutdown();
        } else {
            raiseUp("Shutdown Already");
        }
    }

    private void doShutdown() {
        info.append("shutdown: ").append(Thread.currentThread().getName()).append("; ");
        //#1
        isWhile.set(false); //Говорим закончить
        //#2
        run(); //Выводим из возможного паркинга
        //#3
        isShutdown.set(true);

        long timeOutMs = 1000;
        long startTimeMs = System.currentTimeMillis();
        while (isRun.get()) { //Пытаемся подождать пока потоки самостоятельно закончат свою работу
            Util.sleepMs(timeOutMs / 4);
            if (System.currentTimeMillis() - startTimeMs > timeOutMs) { //Не смогли за отведённое время
                App.context.getBean(ExceptionHandler.class).handler(
                        new RuntimeException(
                                "Thread "
                                        + getClass().getSimpleName()
                                        + " on set isWhile = false timeOut"
                                        + timeOutMs
                                        + "ms. The thread is keep alive")
                );
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
                App.context.getBean(ExceptionHandler.class).handler(
                        new RuntimeException(
                                "Thread "
                                        + getClass().getSimpleName()
                                        + " on interrupt() timeOut"
                                        + timeOutMs
                                        + " ms. The thread is keep alive")
                );
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
        isRun.set(false);
    }

    @Override
    public void polled() {
        countOperation.set(0);
    }
}
