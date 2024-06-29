package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.component.manager.ManagerRateLimit;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public class ThreadResource extends ExpirationMsMutableImpl implements ClassName, Resource<Void, Void> {

    private final Thread thread;

    @Getter
    private final String name;

    private final AtomicBoolean isInit = new AtomicBoolean(false);

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    @Getter
    private final AtomicBoolean isWhile = new AtomicBoolean(true);

    private final AtomicBoolean inPark = new AtomicBoolean(false);

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private final ThreadPoolPromise pool;

    public ThreadResource(String name, ThreadPoolPromise pool) {
        this.pool = pool;
        RateLimit rateLimit = App.get(ManagerRateLimit.class).get(getClassName(pool.getName()));
        rateLimit.init(App.context, RateLimitName.THREAD_TPS.getName(), RateLimitItemInstance.TPS);
        this.name = name;

        thread = new Thread(() -> {
            while (isWhile.get() && isNotInterrupted()) {
                active();
                if (!rateLimit.check(null)) {
                    goToTheParking();
                    continue;
                }
                ExpirationMsImmutableEnvelope<PromiseTask> promiseTask = pool.getPromiseTask();
                if (promiseTask != null) {
                    try {
                        promiseTask.getValue().run();
                    } catch (Exception e) {
                        App.error(e);
                    }
                    continue; // Если таска была - перепрыгиваем pause()
                }
                //Конец итерации цикла -> всегда pause()
                goToTheParking();
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
        App.error(new RuntimeException("action: " + action + "; cause: " + cause));
    }

    private void goToTheParking() {
        if (!isInit.get()) {
            raiseUp("Thread not initialize", "pause()");
            return;
        }
        if (inPark.compareAndSet(false, true)) {
            if (isShutdown.get()) {
                pool.remove(this);
            } else {
                pool.complete(this, null);
                LockSupport.park(thread);
            }
        }
    }

    public boolean run() {
        if (isShutdown.get()) {
            raiseUp("Подавление запуска, поток остановлен", "run()");
            return false;
        } else if (isInit.compareAndSet(false, true)) {
            //Что бы второй раз не получилось запустить поток после остановки проверим на isRun
            if (isRun.compareAndSet(false, true)) {
                thread.start(); //start() - create new thread / run() - Runnable run in main thread
                return true;
            } else {
                raiseUp("Подавление запуска, поток в работе", "run()");
            }
        } else if (inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
            return true;
        }
        return false;
    }

    public boolean shutdown() {
        if (!isInit.get()) {
            raiseUp("Подавление остановки, поток не инициализирован", "shutdown()");
            return false;
        }
        if (isShutdown.compareAndSet(false, true)) { //Что бы больше никто не смог начать останавливать
            doShutdown();
            return true;
        } else {
            raiseUp("Подавление остановки, поток уже остановлен", "shutdown()");
        }
        return false;
    }

    @SuppressWarnings("all")
    private void doShutdown() {
        //#1
        isWhile.set(false); //Говорим закончить
        //#2
        if (inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
        }

        long timeOutMs = 1500; // Так как есть CronManager и по его жизненному циклу нормально спать 1000ms
        long startTimeMs = System.currentTimeMillis();
        while (isRun.get()) { //Пытаемся подождать пока потоки самостоятельно закончат свою работу
            Util.sleepMs(timeOutMs / 4);
            if (System.currentTimeMillis() - startTimeMs > timeOutMs) { //Не смогли за отведённое время
                raiseUp("Поток самостоятельно не закончил работу в течении " + timeOutMs, "doShutdown()");
                break;
            }
        }
        if (isRun.get()) {
            thread.interrupt();
        }
        startTimeMs = System.currentTimeMillis();
        while (isRun.get()) { //Пытаемся подождать пока потоки выйдут от interrupt
            Util.sleepMs(timeOutMs / 4);
            if (System.currentTimeMillis() - startTimeMs > timeOutMs) { //Не смогли за отведённое время
                raiseUp("Поток не закончил работу после interrupt()", "doShutdown()");
                break;
            }
        }
        // Так как мы не можем больше повлиять на остановку
        // В java 22 борльше нет функционала принудительной остановки thread.stop()
        // Таску мы не будем удалять из тайминга - пусть растёт время, а то слишком круто будет новым житься
        pool.remove(this);
        isRun.set(false);
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

    @Override
    public void constructor(NamespaceResourceConstructor constructor) {

    }

    @Override
    public Void execute(Void arguments) {
        return null;
    }

    @Override
    public void close() {
        shutdown();
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
