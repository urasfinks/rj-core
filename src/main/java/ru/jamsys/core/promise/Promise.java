package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.manager.sub.PoolResourceArgument;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.Property;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.extension.trace.TraceTimer;
import ru.jamsys.core.promise.resource.api.PromiseApi;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

// Цепочка обещаний

public interface Promise extends Property<String>, ExpirationMsImmutable, Correlation {

    String getIndex();

    void setIndex(String index);

    void complete(@NonNull PromiseTask task, @NonNull Throwable exception);

    void complete(@Nullable PromiseTask task);

    void complete(@Nullable PromiseTask task, List<PromiseTask> toHead);

    void complete();

    // Синхронное ожидание выполнения Promise
    void await(long timeoutMs);

    // Запускаем цепочку задач от текущего потока
    Promise run();

    Promise setLog(boolean log);

    // Добавление задачи, которая выполнится после успешного завершения цепочки Promise
    Promise onComplete(PromiseTask onComplete);

    default Promise onComplete(PromiseTaskExecuteType promiseTaskExecuteType, Consumer<AtomicBoolean> fn) {
        return onComplete(new PromiseTask("onComplete", this, promiseTaskExecuteType, fn));
    }

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise
    Promise onError(PromiseTask onError);

    default Promise onError(PromiseTaskExecuteType promiseTaskExecuteType, Consumer<AtomicBoolean> fn) {
        return onError(new PromiseTask("onError", this, promiseTaskExecuteType, fn));
    }

    Promise append(PromiseTask task);

    Promise append(PromiseTaskWithResource<?> task);

    default Promise append(String index, PromiseTaskExecuteType promiseTaskExecuteType, Function<AtomicBoolean, List<PromiseTask>> fn) {
        return append(new PromiseTask(index, this, promiseTaskExecuteType, fn));
    }

    default Promise append(String index, PromiseTaskExecuteType promiseTaskExecuteType, Consumer<AtomicBoolean> fn) {
        return append(new PromiseTask(index, this, promiseTaskExecuteType, fn));
    }

    default <T extends Resource<?, ?, ?>> Promise appendWithResource(
            String index,
            PromiseTaskExecuteType promiseTaskExecuteType,
            Class<T> clasResource,
            PoolResourceArgument<T, ?> poolArgument,
            BiConsumer<AtomicBoolean, T> procedure
    ) {
        return append(new PromiseTaskWithResource<>(
                index,
                this,
                promiseTaskExecuteType,
                poolArgument,
                procedure
        ));
    }

    Promise then(PromiseTask task);

    default Promise then(String index, PromiseTaskExecuteType promiseTaskExecuteType, Function<AtomicBoolean, List<PromiseTask>> fn) {
        return then(new PromiseTask(index, this, promiseTaskExecuteType, fn));
    }

    default Promise then(String index, PromiseTaskExecuteType promiseTaskExecuteType, Consumer<AtomicBoolean> fn) {
        return then(new PromiseTask(index, this, promiseTaskExecuteType, fn));
    }

    Promise waits();

    PromiseTask getLastAppendedTask();

    List<Trace<String, Throwable>> getExceptionTrace();

    List<Trace<String, TraceTimer>> getTrace();

    String getLog();

    Promise api(String index, PromiseApi<?> promiseApi);

    boolean isTerminated();

    boolean isException();

    void timeOut(String cause);

}
