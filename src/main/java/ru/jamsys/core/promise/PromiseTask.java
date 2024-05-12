package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.promise.PromiseTaskTime;
import ru.jamsys.core.component.resource.RealThreadComponent;
import ru.jamsys.core.component.resource.VirtualThreadComponent;
import ru.jamsys.core.statistic.expiration.TimeEnvelopeNano;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PromiseTask implements Runnable {

    @Setter
    @Getter
    volatile private boolean complete;

    final PromiseTaskType type;

    // Может порождать дополнительные PromiseTask после выполнения
    // Которые встают в голову стека и будут выполнятся без ожидания
    private Function<AtomicBoolean, List<PromiseTask>> supplier;

    private Consumer<AtomicBoolean> procedure;

    @Getter
    private final Promise promise;

    private int retryCount = 0;

    @Getter
    private int retryDelayMs = 0;

    private final String index;

    public String getIndex() {
        return index + "::" + type.getName();
    }

    // Багу поймал, когда задача на JOIN выполняется в родительском потоке
    // Там нет передачи isThreadRun, только в случаях [IO, COMPUTE] Setter вызывается
    @Setter
    AtomicBoolean isThreadRun = new AtomicBoolean(true);

    public void start() {
        switch (type) {
            case IO -> App.context.getBean(VirtualThreadComponent.class).submit(this);
            case COMPUTE -> App.context.getBean(RealThreadComponent.class).submit(this);
            case JOIN -> run();
            case EXTERNAL_WAIT, EXTERNAL_NO_WAIT ->
                    promise.getTrace().add(new Trace<>(getIndex() + ".start", TraceTimer.getInstanceZero()));
        }
    }

    public void externalComplete() {
        switch (type) {
            case EXTERNAL_WAIT -> {
                promise.getTrace().add(new Trace<>(getIndex() + ".complete", TraceTimer.getInstanceZero()));
                promise.complete(this);
            }
            case EXTERNAL_NO_WAIT -> promise.getTrace().add(new Trace<>(getIndex() + ".complete", TraceTimer.getInstanceZero()));
        }
    }

    public void externalError(Throwable th) {
        switch (type) {
            case EXTERNAL_WAIT -> promise.complete(this, th);
            case EXTERNAL_NO_WAIT -> promise.getExceptionTrace().add(new Trace<>(getIndex() + ".error", th));
        }
    }

    public PromiseTask(String index, Promise promise, PromiseTaskType type) {
        this.index = index;
        this.promise = promise;
        this.type = type;
    }

    public PromiseTask(String index, Promise promise, PromiseTaskType type, Function<AtomicBoolean, List<PromiseTask>> supplier) {
        this.index = index;
        this.promise = promise;
        this.type = type;
        this.supplier = supplier;
    }

    public PromiseTask(String index, Promise promise, PromiseTaskType type, Consumer<AtomicBoolean> procedure) {
        this.index = index;
        this.promise = promise;
        this.type = type;
        this.procedure = procedure;
    }

    public PromiseTask setRetryCount(int count, int delayMs) {
        if (type == PromiseTaskType.JOIN) {
            throw new RuntimeException(
                    this.getClass().getName()
                            + " with type: ["
                            + PromiseTaskType.JOIN.getName()
                            + "] doesn't work with retries"
            );
        }
        this.retryCount = count;
        this.retryDelayMs = delayMs;
        return this;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        TimeEnvelopeNano<String> timer = App.context.getBean(PromiseTaskTime.class).add(index);
        long timeStart = System.nanoTime();
        try {
            if (supplier != null) {
                promise.complete(this, supplier.apply(isThreadRun));
            } else if (procedure != null) {
                procedure.accept(isThreadRun);
                promise.complete(this);
            }
        } catch (Throwable th) {
            App.context.getBean(ExceptionHandler.class).handler(th);
            if (retryCount > 0) {
                retryCount--;
                promise.getExceptionTrace().add(new Trace<>(index, th));
                App.context.getBean(PromiseTaskTime.class).addRetryDelay(this);
            } else {
                promise.complete(this, th);
            }
        }
        timer.stop();
        TraceTimer traceTimer = new TraceTimer(startTime, System.currentTimeMillis(), timer.getOffsetLastActivityNano());
        promise.getTrace().add(new Trace<>(index, traceTimer));
    }
}
