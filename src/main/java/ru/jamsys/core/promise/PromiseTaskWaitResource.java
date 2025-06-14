package ru.jamsys.core.promise;

import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskWithResourceConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.resource.PoolResourceForPromiseTaskWaitResource;

import java.util.concurrent.atomic.AtomicBoolean;

public class PromiseTaskWaitResource<T extends AbstractExpirationResource> extends AbstractPromiseTask {

    private T receiveResource;

    private final PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> executeBlock;

    @SuppressWarnings("all")
    private final ManagerConfiguration<PoolResourceForPromiseTaskWaitResource<T>> poolResourcePromiseTaskWaitConfiguration;

    public PromiseTaskWaitResource(
            String indexTask,
            Promise promise,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> executeBlock,
            ManagerConfiguration<PoolResourceForPromiseTaskWaitResource<T>> poolResourcePromiseTaskWaitConfiguration
    ) {
        super(indexTask, promise, PromiseTaskExecuteType.IO, null);
        this.executeBlock = executeBlock;
        this.poolResourcePromiseTaskWaitConfiguration = poolResourcePromiseTaskWaitConfiguration;
    }

    // Этот блок вызывается из Promise.loop() и подразумевает запуск ::run из внешнего потока
    // Мы его переопределили, добавляя задачу в Pool, а вот уже когда освободится ресурс в пуле
    // Пул сам вызовет start с передачей туда ресурса, там то мы и вызовем::run из внешнего потока
    @Override
    public void prepareLaunch(ProcedureThrowing terminalExecute) {
        this.terminalExecute = terminalExecute;
        PoolResourceForPromiseTaskWaitResource<T> poolResourceForPromiseTaskWaitResource = poolResourcePromiseTaskWaitConfiguration.get();
        getPromise().getTrace().add(new Trace<>(
                getNs() + "::pool[" + poolResourceForPromiseTaskWaitResource.getNs() + "].submitPromiseTask()",
                null
        ));
        poolResourceForPromiseTaskWaitResource.submitPromiseTask(this);
    }

    @Override
    protected void executeProcedure() throws Throwable {
        if (receiveResource != null) {
            try {
                executeBlock.accept(threadRun, this, this.getPromise(), receiveResource);
                poolResourcePromiseTaskWaitConfiguration.get().release(receiveResource, null);
            } catch (Throwable th) {
                poolResourcePromiseTaskWaitConfiguration.get().release(receiveResource, th);
                throw th;
            }
        } else {
            throw new RuntimeException("receiveResource is null");
        }
    }

    @Override
    protected boolean hasProcedure() {
        return executeBlock != null;
    }

    // Пул вызывает этот метод, когда появляется доступный ресурс
    public void onReceiveResource(T poolItem) {
        this.receiveResource = poolItem;
        getPromise()
                .getTrace()
                .add(new Trace<>(getNs()
                        + "::pool[" + poolResourcePromiseTaskWaitConfiguration.get().getNs() + "].onReceiveResource()",
                                null
                        )
                );
        super.prepareLaunch(null);
    }

}
