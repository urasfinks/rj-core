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
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ThreadExecutePromiseTask extends ExpirationMsMutableImplAbstractLifeCycle
        implements Resource<Void, Void>, CascadeKey {

    private Thread thread;

    private final AtomicBoolean spin = new AtomicBoolean(true);

    private final AtomicBoolean threadWork = new AtomicBoolean(true);

    private final ThreadPoolExecutePromiseTask pool;

    private final Manager.Configuration<RateLimitItem> rateLimitConfiguration;

    private final int indexThread;

    @Getter
    private final String ns;

    public ThreadExecutePromiseTask(
            String ns,
            int indexThread,
            ThreadPoolExecutePromiseTask pool,
            Manager.Configuration<RateLimitItem> rateLimitConfiguration
    ) {
        this.ns = ns;
        this.pool = pool;
        this.indexThread = indexThread;
        this.rateLimitConfiguration = rateLimitConfiguration;
    }

    @Override
    public void runOperation() {
        thread = new Thread(() -> {
            threadWork.set(true);
            // При создании экземпляра в ThreadPoolPromiseTask.createPoolItem() происходит автоматически run()
            // При этом Pool его паркует, поэтому нам в ручную надо его LockSupport.park(thread);
            // Если не запарковать, получим: "Этот код не должен был случиться! Проверить логику!"
            // потому что если не выполнить LockSupport.park(thread); поток прокрутит логику и попытается сам
            // запарковаться в пуле, а там мы встретим дубликат и ошибку ествественно
            LockSupport.park(thread);
            try {
                while (spin.get() && !thread.isInterrupted()) {
                    // Обрабатываем ложные срабатывания (spurious wakeups) при использовании LockSupport.park(),
                    // хотя они происходят реже, чем при использовании Object.wait()
                    if (pool.inPark(this)) {
                        LockSupport.park(thread);
                        continue;
                    }
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
                    // На всякий пожарный, если кто-то допишет логику и забудет после паркинга сделать continue;
                    //noinspection UnnecessaryContinue
                    continue;
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
        // Поток должен называться явно с указанием кто он
        thread.setName(getCascadeKey(ns) + "_" + indexThread);
        thread.start();
    }

    @Override
    public void shutdownOperation() {
        spin.set(false); //Говорим закончить
        LockSupport.unpark(thread);
        Util.await(threadWork, 1500, 100, () -> {
            UtilLog.printError("Поток не закончил работу после spin.set(false) -> interrupt()");
            thread.interrupt();
        });
        Util.await(threadWork, 1500, 100, () -> UtilLog.printError(
                "Поток не закончил работу после interrupt()"
        ));
        // Так как мы не можем больше повлиять на остановку
        // В java 22 больше нет функционала принудительной остановки thread.stop()
        // Таску мы не будем удалять из тайминга - пусть растёт время, а то слишком круто будет новым житься
        pool.remove(this);
    }

    @Override
    public void init(String ns) {

    }

    @Override
    public Void execute(Void arguments) {
        // Мы не можем в этом блоке делать никакие проверки, так как execute был вызван, когда ресурс был изъят из пула
        // Нам тут надо либо извращаться с возращением в пул, либо пустить всё своим ходом. То есть никаких проверок
        // тут делать нельзя на подобии: pool.isEmpty() или !threadWork.get()
        LockSupport.unpark(thread);
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

}
