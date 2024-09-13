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
import ru.jamsys.core.extension.trace.TracePromise;
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

    public Promise getPromise() {
        return promise.isDebug() ? new PromiseDebug(promise, this) : promise;
    }

    private int retryCount = 0;

    @Getter
    private int retryDelayMs = 0;

    @ToString.Include
    @Getter
    private final String index;

    protected ProcedureThrowing afterExecuteBlock;

    @Setter
    @Getter
    private boolean terminated = false;

    @Setter
    AtomicBoolean isThreadRun;

    private Long prepare;

    public PromiseTask(String index, Promise promise, PromiseTaskExecuteType type) {
        this.index = index;
        this.promise = promise;
        this.type = type;
    }

    public PromiseTask(String index, Promise promise, PromiseTaskExecuteType type, BiConsumerThrowing<AtomicBoolean, Promise> procedure) {
        this.index = index;
        this.promise = promise;
        this.type = type;
        this.procedure = procedure;
    }

    public void externalComplete() {
        if (Objects.requireNonNull(type) == PromiseTaskExecuteType.EXTERNAL_WAIT) {
            promise.getTrace().add(new TracePromise<>(getIndex() + ".complete", null, type, this.getClass()));
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
        this.afterExecuteBlock = afterExecuteBlock;
        switch (type) {
            case IO, ASYNC_NO_WAIT_IO -> App.get(ServiceThreadVirtual.class).execute(this);
            case COMPUTE, ASYNC_NO_WAIT_COMPUTE -> App.get(ManagerThreadPool.class).addPromiseTask(this);
            case EXTERNAL_WAIT -> promise
                    .getTrace()
                    .add(new TracePromise<>(getIndex() + ".start", null, type, this.getClass()));
        }
    }

    @Getter
    private TracePromise<String, TimerNanoEnvelope<String>> activeTrace;

    // execute current thread
    @Override
    public void run() {
        TimerNanoEnvelope<String> timerEnvelope = App.get(ServicePromise.class).registrationTimer(index);
        activeTrace = new TracePromise<>(
                getIndex(),
                timerEnvelope,
                type,
                this.getClass()
        );
        activeTrace.setPrepare(prepare);
        if (retryCount > 0) {
            activeTrace.setRetry(retryCount);
        }
        promise.getTrace().add(activeTrace);
        Throwable registerThrowable = null;
        boolean retry = false;
        try {
            executeBlock();
        } catch (Throwable th) {
            if (retryCount > 0) {
                retryCount--;
                promise.getExceptionTrace().add(new TracePromise<>(index, th, type, this.getClass()));
                App.get(ServicePromise.class).addRetryDelay(this);
                retry = true;
            } else {
                registerThrowable = th;
            }
        }
        if (afterExecuteBlock != null) {
            try {
                afterExecuteBlock.run();
            } catch (Throwable th) {
                registerThrowable = th;
            }
        }
        timerEnvelope.stop();
        activeTrace.setTimeStop(System.currentTimeMillis());
        if (registerThrowable == null) {
            // Если в повтор не вышли
            if (!retry) {
                promise.complete(this);
            }
        } else {
            switch (type) {
                // Если это методы без ожидания, нам надо самостоятельно зафиксировать их исключения
                // так как complete для них не вызывается
                case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE ->
                        promise.getExceptionTrace().add(new TracePromise<>(index, registerThrowable, type, this.getClass()));
                default -> promise.complete(this, registerThrowable);
            }
        }
    }

    protected void executeBlock() throws Throwable {
        procedure.accept(isThreadRun, getPromise());
    }

}
