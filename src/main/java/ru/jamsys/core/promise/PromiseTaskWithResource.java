package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerPoolTaskWait;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskWithResourceConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.resource.Resource;

import java.util.concurrent.atomic.AtomicBoolean;

public class PromiseTaskWithResource<T extends Resource<?, ?>> extends PromiseTask {

    @Getter
    @Setter
    private PoolItemEnvelope<?, ?, T> poolItemEnvelope;

    private final PromiseTaskWithResourceConsumerThrowing<PromiseTask, AtomicBoolean, Promise, T> procedure;

    private final PoolSettings<T> poolSettings;

    private final ManagerPoolTaskWait managerPoolTaskWait;

    @SuppressWarnings("all")
    public PromiseTaskWithResource(
            String index,
            Promise promise,
            PromiseTaskWithResourceConsumerThrowing<PromiseTask, AtomicBoolean, Promise, T> procedure,
            PoolSettings<T> poolSettings
    ) {
        super(index, promise, PromiseTaskExecuteType.IO, null);
        this.poolSettings = poolSettings;
        this.procedure = procedure;
        managerPoolTaskWait = App.get(ManagerPoolTaskWait.class);
        // Мы не можем так делать, потому что элемент живёт своей жизнью и гаситься AbstractManager
        // по истечению времени, мы должны использовать get() что бы остановленный элемент стартовал перед использованием
        //managerElement = managerPoolTaskWait.get(poolSettings.getIndex(), poolSettings);
    }

    // Этот блок вызывается из Promise.loop() и подразумевает запуск ::run из внешнего потока
    // Мы его переопределили, добавляя задачу в Pool, а вот уже когда освободится ресурс в пуле
    // Пул сам вызовет start с передачей туда ресурса, там то мы и вызовем ::run из внешнего потока
    @Override
    public void prepareLaunch(ProcedureThrowing afterExecuteBlock) {
        this.afterBlockExecution = afterExecuteBlock;
        getPromise().getTrace().add(new Trace<>(getIndex() + ".Pool-Subscribe(" + poolSettings.getIndex() + ")", null));
        managerPoolTaskWait.get(poolSettings.getIndex(), poolSettings).addPromiseTaskPool(this);
    }

    @Override
    protected void executeBlock() throws Throwable {
        try (PoolItemEnvelope<?, ?, T> res = getPoolItemEnvelope()) {
            procedure.accept(threadRun, this, getPromise(), res.getItem());
        }
    }

    // Пул вызывает этот метод
    public void start(PoolItemEnvelope<?, ?, T> poolItem) {
        setPoolItemEnvelope(poolItem);
        getPromise().getTrace().add(new Trace<>(getIndex() + ".Pool-Received(" + poolSettings.getIndex() + ")", null));
        super.prepareLaunch(null);
    }

}
