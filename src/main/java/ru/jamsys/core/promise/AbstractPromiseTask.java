package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceThreadVirtual;
import ru.jamsys.core.component.ServiceTimer;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.resource.thread.ThreadPoolExecutePromiseTask;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ToString(onlyExplicitlyIncluded = true)
@Getter
public abstract class AbstractPromiseTask implements Runnable, WaitQueueElement {

    @ToString.Include
    final PromiseTaskExecuteType type;

    private final PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> procedure;

    private final Promise promise;

    @Setter
    private int retryCount = 0;

    @Setter
    private int retryDelayMs = 1000;

    @ToString.Include
    private final String ns;

    // Используется, когда вызывается терминальный блок, который фиксирует, что Promise закончен.
    // Но есть ещё функция onComplete/onError которую как бы надо выполнить и до тех пор, пока эта функция не выполниться
    // Нельзя Promise устанавливать run.set(false)
    protected ProcedureThrowing terminalExecute;

    // Флаг от потока, который исполняет данную задачу
    @Setter
    protected AtomicBoolean threadRun;

    @JsonIgnore
    private final Manager.Configuration<ThreadPoolExecutePromiseTask> computeThreadConfiguration;

    public AbstractPromiseTask(
            String ns,
            Promise promise,
            PromiseTaskExecuteType type,
            PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> procedure
    ) {
        this.ns = ns;
        this.promise = promise;
        this.type = type;
        this.procedure = procedure;
        computeThreadConfiguration = App.get(Manager.class).configure(
                ThreadPoolExecutePromiseTask.class,
                ns,
                (ns1) -> {
                    ThreadPoolExecutePromiseTask threadPoolExecutePromiseTask = new ThreadPoolExecutePromiseTask(ns1);
                    threadPoolExecutePromiseTask.run();
                    return threadPoolExecutePromiseTask;
                }
        );
    }

    // execute on another thread
    public void prepareLaunch(ProcedureThrowing terminalExecute) {
        this.terminalExecute = terminalExecute;
        if (hasProcedure()) {
            switch (type) {
                case IO, ASYNC_IO -> App.get(ServiceThreadVirtual.class).execute(this);
                case COMPUTE, ASYNC_COMPUTE -> computeThreadConfiguration.get().addPromiseTask(this);
            }
        } else {
            // Исполняемого блока нет, в этом же потоке провернём, что бы статистику записать и всё
            run();
        }
    }

    protected void flushRepositoryChange() {
        if (promise.getLogType() == LogType.DEBUG) {
            if (promise.getRepositoryMap() instanceof PromiseRepositoryDebug promiseRepositoryDebug) {
                Collection<Map<String, Object>> traces = promiseRepositoryDebug.flushChange();
                if (!traces.isEmpty()) {
                    promise.getTrace().add(new Trace<>("RepositoryChange(" + this.getNs() + ")", traces));
                }
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
        getPromise().getTrace().add(new Trace<>(this.getNs() + "::run()", null));
        try {
            executeProcedure();
            // Если эта задача не относится к асинхронным ожиданиям
            if (
                    type != PromiseTaskExecuteType.ASYNC_IO
                            && type != PromiseTaskExecuteType.ASYNC_COMPUTE
            ) {
                getPromise().completePromiseTask(this);
            }
        } catch (Throwable th) {
            if (retryCount > 0) {
                retryCount--;
                App.get(ServicePromise.class).addRetryDelay(this);
                getPromise().getTrace().add(new Trace<>(this.getNs() + "::addRetryDelay(" + retryCount + ")", th));
            } else {
                getPromise().setError(this.getNs(), new ForwardException(th));
            }
        } finally {
            // Используется только в терминальных задачах onComplete / onError для того, что бы перевести Promise
            // run.set(false) и указать terminalStatus
            try {
                if (terminalExecute != null) {
                    terminalExecute.run();
                }
            } catch (Throwable th) {
                getPromise().setError(this.getNs(), new ForwardException(th));
            }
            flushRepositoryChange();
            timerEnvelope.stop();
        }
    }

}
