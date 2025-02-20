package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerRateLimit;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public class ThreadResource extends ExpirationMsMutableImpl implements CascadeName, Resource<Void, Void> {

    private final Thread thread;

    private final AtomicBoolean init = new AtomicBoolean(false);

    protected final AtomicBoolean run = new AtomicBoolean(false);

    @Getter
    private final AtomicBoolean spin = new AtomicBoolean(true);

    private final AtomicBoolean inPark = new AtomicBoolean(false);

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private final ThreadPool pool;

    @Getter
    private final CascadeName parentCascadeName;

    @Getter
    private final String key;

    public ThreadResource(CascadeName parentCascadeName, String key, ThreadPool pool) {
        this.parentCascadeName = parentCascadeName;
        this.key = key;
        this.pool = pool;
        RateLimit rateLimit = App.get(ManagerRateLimit.class).get(getCascadeName());

        thread = new Thread(() -> {
            while (spin.get() && isNotInterrupted() && !shutdown.get()) {
                setActivity();
                if (!rateLimit.check()) {
                    goToTheParking();
                    continue;
                }
                ExpirationMsImmutableEnvelope<PromiseTask> promiseTaskEnvelope = pool.getPromiseTask();
                if (promiseTaskEnvelope != null) {
                    try {
                        PromiseTask promiseTask = promiseTaskEnvelope.getValue();
                        promiseTask.setThreadRun(spin);
                        promiseTask.run();
                    } catch (Exception e) {
                        App.error(e);
                    }
                    continue; // Если таска была - перепрыгиваем pause()
                }
                //Конец итерации цикла -> всегда pause()
                goToTheParking();
            }
            run.set(false);
        });
        thread.setName(getCascadeName());
    }

    public boolean isInit() {
        return init.get();
    }

    public boolean isNotInterrupted() {
        return !thread.isInterrupted();
    }

    private void raiseUp(String cause, String action) {
        App.error(new RuntimeException("action: " + action + "; cause: " + cause));
    }

    private void goToTheParking() {
        if (!init.get()) {
            raiseUp("Thread not initialize", "pause()");
            return;
        }
        if (inPark.compareAndSet(false, true)) {
            if (shutdown.get()) {
                pool.remove(this);
            } else {
                pool.completePoolItem(this, null);
            }
        }
        // В любом случае поток хотят тормознуть, не важно какие статусы сейчас
        LockSupport.park(thread);
    }

    @Override
    public void shutdown() {
        if (!init.get()) {
            raiseUp("Подавление остановки, поток не инициализирован", "shutdown()");
            //return false;
        }
        if (shutdown.compareAndSet(false, true)) { //Что бы больше никто не смог начать останавливать
            doShutdown();
            //return true;
        } else {
            raiseUp("Подавление остановки, поток уже остановлен", "shutdown()");
        }
        //return false;
    }

    @Override
    public void run() {
        if (shutdown.get()) {
            raiseUp("Подавление запуска, поток остановлен", "run()");
            //return false;
        } else if (init.compareAndSet(false, true)) {
            //Что бы второй раз не получилось запустить поток после остановки проверим на isRun
            if (run.compareAndSet(false, true)) {
                thread.start(); //start() - create new thread / run() - Runnable run in main thread
                //return true;
            } else {
                raiseUp("Подавление запуска, поток в работе", "run()");
            }
        } else if (inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
            //return true;
        }
        //return false;
    }

    @SuppressWarnings("all")
    private void doShutdown() {
        //#1
        spin.set(false); //Говорим закончить
        //#2
        if (inPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
        }
        Util.await(run, 1500, "Поток не закончил работу после isWhile.set(false)");
        if (run.get()) {
            // Мы сами не реализуем sleep, но внутренние процеесы его могут реализовать
            thread.interrupt();
        }
        Util.await(run, 1500, "Поток не закончил работу после interrupt()");
        // Так как мы не можем больше повлиять на остановку
        // В java 22 борльше нет функционала принудительной остановки thread.stop()
        // Таску мы не будем удалять из тайминга - пусть растёт время, а то слишком круто будет новым житься
        pool.remove(this);
        run.set(false);
    }

    @Override
    public void setArguments(ResourceArguments resourceArguments) {

    }

    @Override
    public Void execute(Void arguments) {
        run();
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
