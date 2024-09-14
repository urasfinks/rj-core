package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceThreadVirtual;
import ru.jamsys.core.component.manager.ManagerThreadPool;
import ru.jamsys.core.extension.functional.BiConsumerThrowing;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.extension.trace.TracePromiseTask;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem
@ToString(onlyExplicitlyIncluded = true)
public class PromiseTask implements Runnable {

    @ToString.Include
    @Getter
    final PromiseTaskExecuteType type;

    private BiConsumerThrowing<AtomicBoolean, Promise> procedure;

    private final Promise promise;

    private PromiseDebug promiseDebug;

    private int retryCount = 0;

    @Getter
    private int retryDelayMs = 0;

    @ToString.Include
    @Getter
    private final String index;

    // Используется, когда вызывается терминальный блок, который фиксирует, что Promise закончен
    // Но есть ещё функция onComplete/onError которую как бы надо выполнить и до тех пор пока эта функция не выполниться
    // Нельзя Promise устанавливать run.set(false)
    protected ProcedureThrowing afterBlockExecution;

    @Setter
    @Getter
    private boolean terminated = false;

    @Setter
    AtomicBoolean isThreadRun;

    private Long prepare;

    @Getter
    private TracePromiseTask<String, TimerNanoEnvelope<String>> tracePromiseTask;

    public PromiseTask(String index, Promise promise, PromiseTaskExecuteType type) {
        this.index = index;
        this.promise = promise;
        this.type = type;
    }

    public PromiseTask(
            String index,
            Promise promise,
            PromiseTaskExecuteType type,
            BiConsumerThrowing<AtomicBoolean, Promise> procedure
    ) {
        this.index = index;
        this.promise = promise;
        this.type = type;
        this.procedure = procedure;
    }

    public Promise getPromise() {
        if (promise.isDebug()) {
            if (promiseDebug == null) {
                promiseDebug = new PromiseDebug(promise, this);
            }
            return promiseDebug;
        }
        return promise;
    }

    public void externalComplete() {
        if (Objects.requireNonNull(type) == PromiseTaskExecuteType.EXTERNAL_WAIT) {
            // Время можно путём вычитания start.start - complete.start
            promise.getTrace().add(new TracePromiseTask<>(getIndex() + ".complete", null, type, this.getClass()));
            promise.complete(this);
        }
    }

    public void externalError(Throwable th) {
        if (Objects.requireNonNull(type) == PromiseTaskExecuteType.EXTERNAL_WAIT) {
            promise.complete(this, th);
        }
    }

    public PromiseTask setRetryCount(int count, int delayMs) {
        this.retryCount = count;
        this.retryDelayMs = delayMs;
        return this;
    }

    // execute on another thread
    public void prepareLaunch(ProcedureThrowing afterExecuteBlock) {
        this.prepare = System.currentTimeMillis();
        this.afterBlockExecution = afterExecuteBlock;
        switch (type) {
            case IO, ASYNC_NO_WAIT_IO -> App.get(ServiceThreadVirtual.class).execute(this);
            case COMPUTE, ASYNC_NO_WAIT_COMPUTE -> App.get(ManagerThreadPool.class).addPromiseTask(this);
            case EXTERNAL_WAIT -> promise
                    .getTrace()
                    .add(new TracePromiseTask<>(getIndex() + ".start", null, type, this.getClass()));
        }
    }

    // execute current thread
    @Override
    public void run() {
        TimerNanoEnvelope<String> timerEnvelope = App.get(ServicePromise.class).registrationTimer(index);
        tracePromiseTask = new TracePromiseTask<>(index, null, type, this.getClass());
        promise.getTrace().add(tracePromiseTask);

        tracePromiseTask.setNano(timerEnvelope);
        tracePromiseTask.setPrepare(prepare);
        if (retryCount > 0) {
            tracePromiseTask.setRetry(retryCount);
        }
        try {
            executeBlock();
            flushRepositoryChange();
        } catch (Throwable th) {
            flushRepositoryChange();
            stopTimer(timerEnvelope);
            if (retryCount > 0) {
                retryCount--;
                tracePromiseTask.getExceptionTrace().add(new Trace<>(null, th));
                App.get(ServicePromise.class).addRetryDelay(this);
                // Если мы вышли на повтор, то promise.complete вызывать не надо
            } else {
                // Если произошла ошибка в основном блоке, мы не будем выполнять afterBlockExecution
                // Так как нужно придерживаться линейности и быть предсказуемым
                // По сути afterBlockExecution может хотеть использовать данные созданные в executeBlock
                // И тогда можем получить двойную ошибку
                completeThrowable(th);
            }
            return;
        }

        if (afterBlockExecution != null) {
            try {
                afterBlockExecution.run();
            } catch (Throwable th) {
                stopTimer(timerEnvelope);
                completeThrowable(th);
                return;
            }
        }

        stopTimer(timerEnvelope);
        complete();
    }

    private void flushRepositoryChange() {
        if (promise.isDebug()) {
            Promise promiseSource = getPromise();
            if (promiseSource instanceof PromiseDebug) {
                PromiseRepositoryDebug repositoryMap = (PromiseRepositoryDebug) promiseSource.getRepositoryMap();
                repositoryMap.flushRepositoryChange();
            }
        }
    }

    private void stopTimer(TimerNanoEnvelope<String> timerNanoEnvelope) {
        timerNanoEnvelope.stop();
        tracePromiseTask.setTimeStop(System.currentTimeMillis());
    }

    private void complete() {
        switch (type) {
            case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE -> {
            }
            default -> promise.complete(this);
        }
    }

    private void completeThrowable(Throwable th) {
        switch (type) {
            case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE ->
                    tracePromiseTask.getExceptionTrace().add(new Trace<>(null, th));
            default -> promise.complete(this, th);
        }
    }

    protected void executeBlock() throws Throwable {
        procedure.accept(isThreadRun, getPromise());
    }

}
