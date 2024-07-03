package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceThreadVirtual;
import ru.jamsys.core.component.manager.ManagerThreadPool;
import ru.jamsys.core.extension.Procedure;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

public class PromiseTask implements Runnable {

    @Getter
    final PromiseTaskExecuteType type;

    private BiConsumer<AtomicBoolean, Promise> procedure;

    @Getter
    private final Promise promise;

    private int retryCount = 0;

    @Getter
    private int retryDelayMs = 0;

    @Getter
    private final String index;

    protected Procedure beforeExecuteBlock;

    @Setter
    AtomicBoolean isThreadRun;

    public PromiseTask(String index, Promise promise, PromiseTaskExecuteType type) {
        this.index = index;
        this.promise = promise;
        this.type = type;
    }

    public PromiseTask(String index, Promise promise, PromiseTaskExecuteType type, BiConsumer<AtomicBoolean, Promise> procedure) {
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
    public void start(Procedure beforeExecuteBlock) {
        this.beforeExecuteBlock = beforeExecuteBlock;
        switch (type) {
            case IO, ASYNC_NO_WAIT_IO -> App.get(ServiceThreadVirtual.class).execute(this);
            case COMPUTE, ASYNC_NO_WAIT_COMPUTE -> App.get(ManagerThreadPool.class).addPromiseTask(this);
            case EXTERNAL_WAIT -> promise
                    .getTrace()
                    .add(new TracePromise<>(getIndex() + ".start", null, type, this.getClass()));
        }
    }

    // execute current thread
    @Override
    public void run() {
        TimerNanoEnvelope<String> timerEnvelope = App.get(ServicePromise.class).registrationTimer(index);
        TracePromise<String, TimerNanoEnvelope<String>> trace = new TracePromise<>(
                getIndex(),
                timerEnvelope,
                type,
                this.getClass()
        );
        if (retryCount > 0) {
            trace.setRetry(retryCount);
        }
        promise.getTrace().add(trace);
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
        if (beforeExecuteBlock != null) {
            try {
                beforeExecuteBlock.run();
            } catch (Throwable th) {
                registerThrowable = th;
            }
        }
        timerEnvelope.stop();
        trace.setTimeStop(System.currentTimeMillis());
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
