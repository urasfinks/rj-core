package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.manager.RateLimitManager;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ThreadResource extends ExpirationMsMutableImpl implements ClassName, Completable, Resource<Void, Void, Void> {

    private final Thread thread;

    @Getter
    private final String name;

    private final AtomicBoolean isInit = new AtomicBoolean(false);

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    @Getter
    private final AtomicBoolean isWhile = new AtomicBoolean(true);

    private final AtomicBoolean inPark = new AtomicBoolean(false);

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private final StringBuilder info = new StringBuilder();

    private final ThreadPoolPromise pool;

    public ThreadResource(String name, ThreadPoolPromise pool) {
        this.pool = pool;
        RateLimit rateLimit = App.context.getBean(RateLimitManager.class).get(getClassName(pool.getName()));
        rateLimit.init(RateLimitName.THREAD_TPS.getName(), RateLimitItemInstance.TPS);
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

        thread = new Thread(() -> {
            while (isWhile.get() && isNotInterrupted()) {
                active();
                if (!rateLimit.check(null)) {
                    pause();
                    continue;
                }
                ExpirationMsImmutableEnvelope<PromiseTask> promiseTask = pool.getPromiseTask();
                if (promiseTask != null) {
                    try {
                        promiseTask.getValue().run();
                    } catch (Exception e) {
                        App.context.getBean(ExceptionHandler.class).handler(e);
                    }
                    continue; // Если таска была - перепрыгиваем pause()
                }
                //Конец итерации цикла -> всегда pause()
                pause();
            }
            isRun.set(false);
        });
        thread.setName(name);
    }

    public boolean isInit() {
        return isInit.get();
    }

    public boolean isNotInterrupted() {
        return !thread.isInterrupted();
    }

    private void raiseUp(String cause, String action) {
        App.context.getBean(ExceptionHandler.class).handler(
                new RuntimeException("class: " + getClassName()
                        + "; action: " + action
                        + "; cause: " + cause + " \r\n"
                        + info)
        );
    }

    private boolean pause() {
        if (!isInit.get()) {
            raiseUp("Thread not initialize", "pause()");
            return false;
        }
        if (inPark.compareAndSet(false, true)) {
            if (isShutdown.get()) {
                pool.remove(this);
            } else {
                pool.complete(this, null);
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
        } else if (isInit.compareAndSet(false, true)) {
            //Что бы второй раз не получилось запустить поток после остановки проверим на isWhile
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

    public boolean shutdown() {
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

    @SuppressWarnings("all")
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

        long timeOutMs = 1500; // Так как есть CronManager и по его жизненному циклу нормально спать 1000ms
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
            //TODO: удалить из менаджера контроля тасками, что бы не текло время по этой таске для корректного рассчёта
            // распределения тредов по задачам
        }
        pool.remove(this);
        isRun.set(false);
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("isInit: ").append(isInit.get()).append("; ");
        sb.append("isRun: ").append(isRun.get()).append("; ");
        sb.append("isWhile: ").append(isWhile.get()).append("; ");
        sb.append("inPark: ").append(inPark.get()).append("; ");
        sb.append("isShutdown: ").append(isShutdown.get()).append("; ");
        return sb.toString();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

    @Override
    public void constructor(Void constructor) {

    }

    @Override
    public Void execute(Void arguments) {
        return null;
    }

    @Override
    public void close() {
        shutdown();
    }

}
