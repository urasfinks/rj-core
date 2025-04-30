package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceThreadVirtual;
import ru.jamsys.core.component.ServiceTimer;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.resource.thread.PoolThreadPromiseTask;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;


@Getter
public abstract class AbstractPromiseTask implements Runnable, WaitQueueElement {

    final PromiseTaskExecuteType type;

    private final PromiseTaskConsumerThrowing<AbstractPromiseTask, AtomicBoolean, Promise> procedure;

    private final Promise promise;

    @Setter
    private int retryCount = 0;

    @Setter
    private int retryDelayMs = 1000;

    private final String ns;

    // Используется, когда вызывается терминальный блок, который фиксирует, что Promise закончен.
    // Но есть ещё функция onComplete/onError которую как бы надо выполнить и до тех пор, пока эта функция не выполниться
    // Нельзя Promise устанавливать run.set(false)
    protected ProcedureThrowing afterBlockExecution;

    // Флаг от потока, который исполняет данную задачу
    @Setter
    protected AtomicBoolean threadRun;

    private final Manager.Configuration<PoolThreadPromiseTask> configure;

    public AbstractPromiseTask(
            String ns,
            Promise promise,
            PromiseTaskExecuteType type,
            PromiseTaskConsumerThrowing<AbstractPromiseTask, AtomicBoolean, Promise> procedure
    ) {
        this.ns = ns;
        this.promise = promise;
        this.type = type;
        this.procedure = procedure;
        configure = App.get(Manager.class).configure(
                PoolThreadPromiseTask.class,
                ns,
                (ns1) -> {
                    PoolThreadPromiseTask poolThreadPromiseTask = new PoolThreadPromiseTask(ns1);
                    poolThreadPromiseTask.run();
                    return poolThreadPromiseTask;
                }
        );
    }

    // execute on another thread
    public void prepareLaunch(ProcedureThrowing afterExecuteBlock) {
        this.afterBlockExecution = afterExecuteBlock;
        if (hasProcedure()) {
            switch (type) {
                case IO, EXTERNAL_WAIT_IO -> App.get(ServiceThreadVirtual.class).execute(this);
                case COMPUTE, EXTERNAL_WAIT_COMPUTE -> configure.get().addPromiseTask(this);
            }
        } else {
            // Исполняемого блока нет, в этом же потоке провернём, что бы статистику записать и всё
            run();
        }
    }

    protected void flushRepositoryChange() {
        if (promise.getLogType() == LogType.DEBUG) {
            if (promise.getRepositoryMap() instanceof PromiseRepositoryDebug promiseRepositoryDebug) {
                Collection<Trace<String, ?>> traces = promiseRepositoryDebug.flushChange();
                promise.getTrace().add(new Trace<>("RepositoryChange(" + this.getNs() + ")", traces));
            }
        }
    }

    protected void executeProcedure() throws Throwable {
        if (procedure != null) {
            procedure.accept(threadRun, this, promise);
        }
    }

    // Так как может быть наследование от этого класса, то исполняемый блок может быть переопределён
    // Как это уже сделано в PromiseTaskWithResource, дадим потомкам переопределить поведение
    protected boolean hasProcedure() {
        return procedure != null;
    }

    @Override
    public boolean isWait() {
        return type == PromiseTaskExecuteType.WAIT;
    }

    @Override
    public void run() {
        TimerNanoEnvelope<String> timerEnvelope = App.get(ServiceTimer.class).get(this.getNs());
        getPromise().getTrace().add(new Trace<>(this.getNs() + ".run()", null));
        try {
            executeProcedure();
            if (afterBlockExecution != null) {
                afterBlockExecution.run();
            }
            getPromise().completePromiseTask(this);
        } catch (Throwable th) {
            getPromise().getTrace().add(new Trace<>(this.getNs(), th));
            if (retryCount > 0) {
                retryCount--;
                App.get(ServicePromise.class).addRetryDelay(this);
                getPromise().getTrace().add(new Trace<>(this.getNs() + ".addRetryDelay(" + retryCount + ")", null));
            } else {
                getPromise().setError(this.getNs(), th);
            }
        } finally {
            flushRepositoryChange();
            timerEnvelope.stop();
        }
    }

}
