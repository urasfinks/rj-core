package ru.jamsys.thread;

import lombok.Setter;
import lombok.ToString;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.component.TaskManager;
import ru.jamsys.pool.AbstractPool;
import ru.jamsys.pool.Pool;
import ru.jamsys.statistic.AbstractExpired;
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
public class ThreadEnvelope extends AbstractExpired {

    private final Thread thread;

    @ToString.Include
    private final Pool<ThreadEnvelope> pool;

    private final AtomicBoolean isRun = new AtomicBoolean(false);

    private final AtomicBoolean isWhile = new AtomicBoolean(false);

    private final AtomicBoolean inPark = new AtomicBoolean(false);

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    @Setter
    private int maxCountIteration = 100; //Защита от бесконечных задач

    private final AtomicInteger countOperation = new AtomicInteger(0);

    public boolean isNotInterrupted() {
        return !thread.isInterrupted();
    }

    public ThreadEnvelope(String name, Pool<ThreadEnvelope> pool, RateLimitItem rateLimitItem, BiFunction<AtomicBoolean, ThreadEnvelope, Boolean> consumer) {
        this.pool = pool;
        thread = new Thread(() -> {
            AbstractPool.contextPool.set(pool);
            while (isWhile.get() && isNotInterrupted()) {
                active();
                boolean isContinue = false;
                try {
                    isContinue = consumer.apply(isWhile, this);
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
                // countOperation - защита от бесконечных задач
                // Предположим, что поменялось максимальное кол-во потоков и надо срезать потоки
                // Остановкой даём возможность подрезать нагрузку через пул
                if (!isContinue || countOperation.get() > maxCountIteration || !rateLimitItem.checkTps()) {
                    pause();
                }
                countOperation.incrementAndGet();
            }
            isRun.set(false);
        });
        thread.setName(name);
    }

    private void alreadyShutdown() {
        App.context.getBean(ExceptionHandler.class).handler(new RuntimeException(getClass().getSimpleName() + " thread already stop"));
    }

    private void pause() {
        if (isShutdown.get()) {
            alreadyShutdown();
            return;
        }
        if (inPark.compareAndSet(false, true)) {
            pool.complete(this, null);
            LockSupport.park(thread);
        }
    }

    public void run() {
        if (isShutdown.get()) {
            alreadyShutdown();
            return;
        }
        //Что бы второй раз не получилось запустить поток после остановки проверим на isWhile
        if (isRun.compareAndSet(false, true)) {
            isWhile.set(true);
            thread.start(); //start() - create new thread / run() - Runnable run in main thread
        } else if (inPark.compareAndSet(true, false)) {
            countOperation.set(0);
            LockSupport.unpark(thread);
        }
    }

    synchronized public void shutdown() {
        if (isShutdown.get()) {
            alreadyShutdown();
            return;
        }
        //#1
        isWhile.set(false); //Говорим закончить
        //#2
        run(); //Выводим из возможного паркинга
        //#3
        isShutdown.set(true); //ЧТо бы больше никто не смог начать останавливать

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

}
