package ru.jamsys.core.resource.thread;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerRateLimit;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public class ThreadResourcePromiseTask extends ExpirationMsMutableImplAbstractLifeCycle implements Resource<Void, Void> {

    private Thread thread;

    private final AtomicBoolean spin = new AtomicBoolean(true);

    private final AtomicBoolean threadWork = new AtomicBoolean(true);

    private final ThreadPoolPromiseTask pool;

    private final RateLimit rateLimit;

    private final int indexThread;

    @Getter
    private final String key;

    public ThreadResourcePromiseTask(String key, int indexThread, ThreadPoolPromiseTask pool) {
        this.key = key;
        this.pool = pool;
        this.indexThread = indexThread;
        // RateLimit будем запрашивать через родительское каскадное имя, так как key для потока - это
        // всего лишь имя, а поток должен подчиняться правилам (лимитам) пула
        rateLimit = App.get(ManagerRateLimit.class).get(key);
    }

    @Override
    public void runOperation() {
        thread = new Thread(() -> {
            threadWork.set(true);
            // При создании экземпляра в ThreadPoolPromiseTask.createPoolItem() происходит автоматически run()
            // Это означает, что поток начинает крутиться и после того, как задач больше нет - он выполняет
            // pool.completePoolItem(this, null); и происходит "Этот код не должен был случиться! Проверить логику!"
            // так как поток ещё из парка не изымали, а мы без разрешения стартанули сами и пытаемся ещё раз себя
            // закинуть в парк, поэтому сразу блокируемся
            LockSupport.park(thread);
            try {
                while (spin.get() && !thread.isInterrupted()) {
                    setActivity();
                    if (!rateLimit.check()) {
                        pool.completePoolItem(this, null);
                        LockSupport.park(thread);
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
                        continue; // Если таска была - перепрыгиваем toParking()
                    }
                    //Конец итерации цикла -> всегда pause()
                    pool.completePoolItem(this, null);
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
        thread.setName(key + "_" + indexThread);
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
    public void setArguments(ResourceArguments resourceArguments) {

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
