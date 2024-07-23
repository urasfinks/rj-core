package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerPoolTaskWait;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.functional.Procedure;
import ru.jamsys.core.extension.functional.TriConsumer;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.PoolTaskWait;

import java.util.concurrent.atomic.AtomicBoolean;

public class PromiseTaskWithResource<T extends Resource<?, ?>> extends PromiseTask {

    @Getter
    @Setter
    private PoolItemEnvelope<?, ?, T> poolItemEnvelope;

    private final TriConsumer<AtomicBoolean, Promise, T> procedure;

    private final PoolTaskWait<?, ?, ?> managerElement;

    private final PoolSettings<T> poolSettings;

    @SuppressWarnings("all")
    public PromiseTaskWithResource(
            String index,
            Promise promise,
            TriConsumer<AtomicBoolean, Promise, T> procedure,
            PoolSettings<T> poolSettings
    ) {
        super(index, promise, App.getResourceExecutor());
        this.poolSettings = poolSettings;
        this.procedure = procedure;
        ManagerPoolTaskWait managerPoolTaskWait = App.get(ManagerPoolTaskWait.class);
        managerElement = managerPoolTaskWait.get(poolSettings.getIndex(), poolSettings);
    }

    // Этот блок вызывается из Promise.loop() и подразумевает запуск ::run из внешнего потока
    // Мы его переопределили, добавляя задачу в Pool, а вот уже когда освободится ресурс в пуле
    // Пул сам вызовет start с передачей туда ресурса, там то мы и вызовем ::run из внешнего потока
    @Override
    public void prepareLaunch(Procedure afterExecuteBlock) {
        this.afterExecuteBlock = afterExecuteBlock;
        getPromise().getTrace().add(new TracePromise<>(getIndex() + ".Pool-Subscribe(" + poolSettings.getIndex() + ")", null, null, null));
        managerElement.addPromiseTaskPool(this);
    }

    @Override
    protected void executeBlock() throws Throwable {
        try (PoolItemEnvelope<?, ?, T> res = getPoolItemEnvelope()) {
            procedure.accept(isThreadRun, getPromise(), res.getItem());
        }
    }

    // Пул вызывает этот метод
    public void start(PoolItemEnvelope<?, ?, T> poolItem) {
        setPoolItemEnvelope(poolItem);
        getPromise().getTrace().add(new TracePromise<>(getIndex() + ".Pool-Received(" + poolSettings.getIndex() + ")", null, null, null));
        super.prepareLaunch(null);
    }

}
