package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceThreadVirtual;
import ru.jamsys.core.component.manager.ManagerThreadPool;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskConsumerThrowing;
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

    private final PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> procedure;

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

    // Терминальный блок onComplete или onError - особый вид поведения afterBlockExecution при ошибках
    @Setter
    @Getter
    private boolean terminated = false;

    @Setter
    protected AtomicBoolean threadRun;

    private Long prepare;

    @Getter
    private TracePromiseTask<String, TimerNanoEnvelope<String>> tracePromiseTask;

    public PromiseTask(
            String index,
            Promise promise,
            PromiseTaskExecuteType type,
            PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> procedure
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
        if (isExternal()) {
            // Время можно путём вычитания start.start - complete.start
            promise.getTrace().add(new TracePromiseTask<>(getIndex() + ".complete", null, type, this.getClass()));
            promise.completePromise(this);
        }
    }

    public boolean isExternal() {
        return Objects.requireNonNull(type) == PromiseTaskExecuteType.EXTERNAL_WAIT_IO
                || Objects.requireNonNull(type) == PromiseTaskExecuteType.EXTERNAL_WAIT_COMPUTE;
    }

    public void externalError(Throwable th) {
        if (isExternal()) {
            promise.completePromise(this, th);
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
        if (!isExecuteBlockEmpty()) {
            switch (type) {
                case IO, ASYNC_NO_WAIT_IO, EXTERNAL_WAIT_IO -> App.get(ServiceThreadVirtual.class).execute(this);
                case COMPUTE, ASYNC_NO_WAIT_COMPUTE, EXTERNAL_WAIT_COMPUTE ->
                        App.get(ManagerThreadPool.class).addPromiseTask(this);
            }
        } else {
            // Исполняемого блока нет, в этом же потоке провернём, что бы статистику записать и всё
            run();
        }
    }

    // execute current thread
    @Override
    public void run() {
        // Мы должны проверить, что Promise к которому принадлежит это задание ещё не остановлено
        if (!promise.isRun()) {
            completePromiseTaskThrowable(new RuntimeException("Promise is not run"));
        }
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
            // Если не терминальный блок
            if (!isTerminated()) {
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
                    completePromiseTaskThrowable(th);
                }
                return;
            } else {
                // Если терминальный, то просто фиксируем ошибку и всё, тут уже ничего не поделать
                // Ну допустим onComplete вызывает ошибку - ну ведь мы ничего не можем с этим сделать
                // Аналогично с onError - ну ошибка, ну дальше-то что?
                promise.setError(th);
                // Так как можем не увидеть ошибку, дополнительно её выведем
                App.error(th);
            }
        }

        if (afterBlockExecution != null) {
            try {
                afterBlockExecution.run();
            } catch (Throwable th) {
                stopTimer(timerEnvelope);
                completePromiseTaskThrowable(th);
                return;
            }
        }

        stopTimer(timerEnvelope);
        if (!isExternal()) {
            completePromiseTask();
        }
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

    private void completePromiseTask() {
        switch (type) {
            case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE -> {
            }
            default -> promise.completePromise(this);
        }
    }

    private void completePromiseTaskThrowable(Throwable th) {
        switch (type) {
            case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE ->
                    tracePromiseTask.getExceptionTrace().add(new Trace<>(null, th));
            default -> promise.completePromise(this, th);
        }
    }

    protected void executeBlock() throws Throwable {
        if (procedure != null) {
            procedure.accept(threadRun, this, getPromise());
        }
    }

    // Так как может быть наследование от этого класса, то исполняемый блок может быть переопределён
    // Как это уже сделано в PromiseTaskWithResource, дадим потомкам переопределить поведение
    protected boolean isExecuteBlockEmpty() {
        return procedure == null;
    }

}
