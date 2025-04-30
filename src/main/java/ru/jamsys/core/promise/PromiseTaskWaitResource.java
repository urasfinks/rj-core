package ru.jamsys.core.promise;

import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskWithResourceConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.pool.PoolItemCompletable;
import ru.jamsys.core.resource.PoolResourcePromiseTaskWaitResource;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.concurrent.atomic.AtomicBoolean;

public class PromiseTaskWaitResource<T extends ExpirationMsMutable & Resource<?, ?>> extends AbstractPromiseTask {

    private PoolItemCompletable<T> receiveResource;

    private final PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> executeBlock;

    @SuppressWarnings("all")
    private final Manager.Configuration<PoolResourcePromiseTaskWaitResource> poolResourcePromiseTaskWaitConfiguration;

    @SuppressWarnings("all")
    public PromiseTaskWaitResource(
            String indexTask,
            Promise promise,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> executeBlock,
            Manager.Configuration<PoolResourcePromiseTaskWaitResource> poolResourcePromiseTaskWaitConfiguration
    ) {
        super(indexTask, promise, PromiseTaskExecuteType.IO, null);
        this.executeBlock = executeBlock;
        this.poolResourcePromiseTaskWaitConfiguration = poolResourcePromiseTaskWaitConfiguration;
    }

    // Этот блок вызывается из Promise.loop() и подразумевает запуск ::run из внешнего потока
    // Мы его переопределили, добавляя задачу в Pool, а вот уже когда освободится ресурс в пуле
    // Пул сам вызовет start с передачей туда ресурса, там то мы и вызовем ::run из внешнего потока
    @SuppressWarnings("all")
    @Override
    public void prepareLaunch(ProcedureThrowing afterExecuteBlock) {
        this.afterBlockExecution = afterExecuteBlock;
        PoolResourcePromiseTaskWaitResource poolResourcePromiseTaskWaitResource = poolResourcePromiseTaskWaitConfiguration.get();
        getPromise().getTrace().add(new Trace<>(
                this.getNs() + ".Pool-Subscribe(" + poolResourcePromiseTaskWaitResource.getKey() + ")",
                null
        ));
        poolResourcePromiseTaskWaitResource.addPromiseTask(this);
    }

    @Override
    protected void executeProcedure() throws Throwable {
        if (receiveResource != null) {
            try {
                executeBlock.accept(threadRun, this, this.getPromise(), receiveResource.getItem());
            } catch (Throwable th) {
                receiveResource.setThrowable(th);
                throw th;
            } finally {
                receiveResource.close();
            }
        } else {
            throw new RuntimeException("receiveResource is null");
        }
    }

    @Override
    protected boolean hasProcedure() {
        return executeBlock == null;
    }

    // Пул вызывает этот метод, когда появляется доступный ресурс
    public void onReceiveResource(PoolItemCompletable<T> poolItem) {
        this.receiveResource = poolItem;
        getPromise().getTrace().add(new Trace<>(this.getNs() + ".Pool-Receive(" + poolResourcePromiseTaskWaitConfiguration.get().getKey() + ")", null));
        super.prepareLaunch(null);
    }

}
