package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceThreadReal;
import ru.jamsys.core.component.ServiceThreadVirtual;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.statistic.timer.Timer;

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

    // Багу поймал, когда задача на JOIN выполняется в родительском потоке
    // Там нет передачи isThreadRun, только в случаях [IO, COMPUTE] Setter вызывается
    @Setter
    AtomicBoolean isThreadRun = new AtomicBoolean(true);

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
        if (type == PromiseTaskExecuteType.JOIN) {
            throw new RuntimeException(
                    this.getClass().getName()
                            + " with type: ["
                            + PromiseTaskExecuteType.JOIN.getName()
                            + "] doesn't work with retries"
            );
        }
        this.retryCount = count;
        this.retryDelayMs = delayMs;
        return this;
    }

    // execute on another thread
    public void start() {
        switch (type) {
            case IO, ASYNC_NO_WAIT_IO -> App.get(ServiceThreadVirtual.class).execute(this);
            case COMPUTE, ASYNC_NO_WAIT_COMPUTE -> App.get(ServiceThreadReal.class).execute(this);
            case JOIN -> run();
            case EXTERNAL_WAIT ->
                    promise.getTrace().add(new TracePromise<>(getIndex() + ".start", null, type, this.getClass()));
        }
    }

    // execute current thread
    @Override
    public void run() {
        PropertyEnvelope<Timer, String, String> timerEnvelope = App.get(ServicePromise.class).registrationTimer(index);
        timerEnvelope.setProperty("type", type.getName());
        Timer timer = timerEnvelope.getValue();
        TracePromise<String, Timer> trace = new TracePromise<>(getIndex(), null, type, this.getClass());
        promise.getTrace().add(trace);
        boolean isNormal = false;
        try {
            executeBlock();
            isNormal = true;
        } catch (Throwable th) {
            App.error(th);
            if (retryCount > 0) {
                retryCount--;
                promise.getExceptionTrace().add(new TracePromise<>(index, th, type, this.getClass()));
                App.get(ServicePromise.class).addRetryDelay(this);
            } else {
                switch (type) {
                    case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE ->
                            promise.getExceptionTrace().add(new TracePromise<>(index, th, type, this.getClass()));
                    default -> promise.complete(this, th);
                }
            }
        }
        timer.stop();
        trace.setValue(timer);
        if (isNormal) {
            getPromise().complete(this);
        }
    }

    protected void executeBlock() throws Throwable {
        procedure.accept(isThreadRun, getPromise());
    }

}
