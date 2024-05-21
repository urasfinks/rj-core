package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.promise.resource.api.PromiseApi;
import ru.jamsys.core.extension.Property;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

// Цепочка обещаний

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Promise extends Property<String>, ExpirationMsImmutable {

    String getIndex();

    void setIndex(String index);

    String getCorrelation();

    void complete(@NonNull PromiseTask task, @NonNull Throwable exception);

    void complete(@Nullable PromiseTask task);

    void complete(@Nullable PromiseTask task, List<PromiseTask> toHead);

    void complete();

    boolean inProgress();

    // Синхронное ожидание выполнения Promise
    void await(long timeoutMs);

    // Запускаем цепочку задач от текущего потока
    Promise run();

    Promise setCorrelation(String rqUid);

    Promise setLog(boolean log);

    // Добавление задачи, которая выполнится после успешного завершения цепочки Promise
    Promise onComplete(PromiseTask onComplete);

    default Promise onComplete(PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn) {
        return onComplete(new PromiseTask("onComplete", this, promiseTaskType, fn));
    }

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise
    Promise onError(PromiseTask onError);

    default Promise onError(PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn) {
        return onError(new PromiseTask("onError", this, promiseTaskType, fn));
    }

    Promise append(PromiseTask task);

    default Promise append(String index, PromiseTaskType promiseTaskType, Function<AtomicBoolean, List<PromiseTask>> fn) {
        return append(new PromiseTask(index, this, promiseTaskType, fn));
    }

    default Promise append(String index, PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn) {
        return append(new PromiseTask(index, this, promiseTaskType, fn));
    }

    Promise then(PromiseTask task);

    default Promise then(String index, PromiseTaskType promiseTaskType, Function<AtomicBoolean, List<PromiseTask>> fn) {
        return then(new PromiseTask(index, this, promiseTaskType, fn));
    }

    default Promise then(String index, PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn) {
        return then(new PromiseTask(index, this, promiseTaskType, fn));
    }

    Promise waits();

    PromiseTask getLastAppendedTask();

    List<Trace<String, Throwable>> getExceptionTrace();

    List<Trace<String, TraceTimer>> getTrace();

    String getLog();

    Promise api(String index, PromiseApi<?> promiseApi);

    boolean isCompleted();

    boolean isException();

    void timeOut(String cause);

}
