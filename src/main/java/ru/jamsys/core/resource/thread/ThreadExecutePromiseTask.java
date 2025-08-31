package ru.jamsys.core.resource.thread;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.rate.limit.tps.RateLimitTps;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.promise.AbstractPromiseTask;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ThreadExecutePromiseTask extends AbstractExpirationResource {

    private Thread thread;

    private final AtomicBoolean spin = new AtomicBoolean(true);

    private final AtomicBoolean threadWork = new AtomicBoolean(true);

    private PoolThreadExecutePromiseTask pool;

    private ManagerConfiguration<RateLimitTps> rateLimitConfiguration;

    private Integer indexThread;

    @Getter
    private final String ns;

    @Getter
    private final String key;

    public ThreadExecutePromiseTask(String ns, String key) {
        this.ns = ns;
        this.key = key;
    }

    public void setup(
            PoolThreadExecutePromiseTask pool,
            ManagerConfiguration<RateLimitTps> rateLimitConfiguration,
            Integer indexThread
    ) {
        this.pool = pool;
        this.rateLimitConfiguration = rateLimitConfiguration;
        this.indexThread = indexThread;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                ;
    }

    @Override
    public void runOperation() {
        if (pool == null) {
            throw new RuntimeException("pool is null");
        }
        if (rateLimitConfiguration == null) {
            throw new RuntimeException("rateLimitConfiguration is null");
        }
        if (indexThread == null) {
            throw new RuntimeException("indexThread is null");
        }
        // Жизненный цикла while может прокручиваться до 3-х раз в 1 секунду при наличии 1 задачи
        // 1 оборот: Пришла таска, мы её обрабатываем и continue
        // 2 оборот: pool.getPromiseTask() получаем null и уходим на парковку
        // 3 приходит Helper3s->manager.helper()->serviceBell()
        // Поэтому видно как поток по обслуживанию Helper1s.ExpirationList на TPS показывает 2-3
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
                        pool.release(this, null);
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
                    pool.release(this, null);
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
                // pool.remove(this); - если не вернуть элемент в пул, он не будет никому возвращаться, он протухнет и
                // автоматом будет выкинут из пула
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
        Util.await(
                1500,
                100,
                () -> !threadWork.get(),
                null,
                () -> {
                    UtilLog.printError("Поток не закончил работу после spin.set(false) -> interrupt() " + thread.getName());
                    Util.printThreadStackTrace();
                    thread.interrupt();
                }
        );

        Util.await(
                1500,
                100,
                () -> !threadWork.get(),
                null,
                () -> UtilLog.printError("Поток не закончил работу после interrupt() " + thread.getName())
        );

        // Так как мы не можем больше повлиять на остановку
        // В java 22 больше нет функционала принудительной остановки thread.stop()
        // Таску мы не будем удалять из тайминга - пусть растёт время, а то слишком круто будет новым житься
        // pool.remove(this); - автоматическое выбрасывание из пула протухших элементов
    }

    public Void execute() {
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
