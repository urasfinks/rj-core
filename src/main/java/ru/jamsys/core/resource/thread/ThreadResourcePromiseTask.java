package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceConfiguration;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public class ThreadResourcePromiseTask extends ExpirationMsMutableImplAbstractLifeCycle
        implements Resource<Void, Void>, CascadeKey {

    private Thread thread;

    private final AtomicBoolean spin = new AtomicBoolean(true);

    private final AtomicBoolean threadWork = new AtomicBoolean(true);

    private final PoolThreadPromiseTask pool;

    private final Manager.Configuration<RateLimitItem> rateLimitConfiguration;

    private final int indexThread;

    @Getter
    private final String ns;

    public ThreadResourcePromiseTask(String ns, int indexThread, PoolThreadPromiseTask pool) {
        this.ns = ns;
        this.pool = pool;
        this.indexThread = indexThread;
        // RateLimit будем запрашивать через родительское каскадное имя, так как key для потока - это
        // всего лишь имя, а поток должен подчиняться правилам (лимитам) пула
        rateLimitConfiguration = App.get(Manager.class).configure(RateLimitItem.class, ns);
    }

    @Override
    public void runOperation() {
        thread = new Thread(() -> {
            //System.out.println("!! "+(key + "_" + indexThread));
            threadWork.set(true);
            // При создании экземпляра в ThreadPoolPromiseTask.createPoolItem() происходит автоматически run()
            // Это означает, что поток начинает крутиться и после того, как задач больше нет - он выполняет
            // pool.completePoolItem(this, null); и происходит "Этот код не должен был случиться! Проверить логику!"
            // так как поток ещё из парка не изымали, а мы без разрешения стартанули сами и пытаемся ещё раз себя
            // закинуть в парк, поэтому сразу блокируемся
            LockSupport.park(thread);
            try {
                while (spin.get() && !thread.isInterrupted()) {
                    markActive();
                    if (!rateLimitConfiguration.get().check()) {
                        pool.releasePoolItem(this, null);
                        LockSupport.park(thread);
                        continue;
                    }
                    ExpirationMsImmutableEnvelope<AbstractPromiseTask> promiseTaskEnvelope = pool.getPromiseTask();

                    if (promiseTaskEnvelope != null) {
                        try {
                            AbstractPromiseTask promiseTask = promiseTaskEnvelope.getValue();
                            promiseTask.setThreadRun(spin);
                            promiseTask.run();
                        } catch (Exception e) {
                            App.error(e);
                        }
                        continue; // Если таска была - перепрыгиваем toParking()
                    }
                    //Конец итерации цикла -> всегда pause()
                    pool.releasePoolItem(this, null);
                    LockSupport.park(thread);
                }
            } catch (Throwable th) {
                App.error(th);
            } finally {
                // Это если что-то пойдёт не так и поток перестанет работать не потому что вызвали shutdownOperation
                // надо его удалить из пула, он всё равно уже не рабочий
                pool.remove(this);
                threadWork.set(false);
            }
        });
        thread.setName(getCascadeKey(ns) + "_" + indexThread);
        thread.start();
    }

    @Override
    public void shutdownOperation() {
        spin.set(false); //Говорим закончить
        LockSupport.unpark(thread);
        Util.await(threadWork, 1500, 100, () -> {
            UtilLog.printError(ThreadResourcePromiseTask.class, "Поток не закончил работу после spin.set(false) -> interrupt()");
            thread.interrupt();
        });
        Util.await(threadWork, 1500, 100, () -> UtilLog.printError(
                ThreadResourcePromiseTask.class,
                "Поток не закончил работу после interrupt()"
        ));
        // Так как мы не можем больше повлиять на остановку
        // В java 22 больше нет функционала принудительной остановки thread.stop()
        // Таску мы не будем удалять из тайминга - пусть растёт время, а то слишком круто будет новым житься
        pool.remove(this);
    }

    @Override
    public void init(ResourceConfiguration resourceConfiguration) {

    }

    @Override
    public Void execute(Void arguments) {
        LockSupport.unpark(thread);
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
