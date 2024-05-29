package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.Property;
import ru.jamsys.core.extension.TriConsumer;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.extension.trace.TraceTimer;
import ru.jamsys.core.promise.resource.extension.PromiseExtension;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

// Цепочка обещаний

public interface Promise extends Property<String>, ExpirationMsImmutable, Correlation {

    String getIndex();

    void setIndex(String index);

    void complete(@NonNull PromiseTask task, @NonNull Throwable exception);

    void complete(@Nullable PromiseTask task);

    void complete();

    // Синхронное ожидание выполнения Promise
    void await(long timeoutMs);

    // Запускаем цепочку задач от текущего потока
    Promise run();

    // Добавление задачи, которая выполнится после успешного завершения цепочки Promise
    Promise onComplete(PromiseTask onComplete);

    default Promise onComplete(PromiseTaskExecuteType promiseTaskExecuteType, BiConsumer<AtomicBoolean, Promise> fn) {
        return onComplete(new PromiseTask("onComplete", this, promiseTaskExecuteType, fn));
    }

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise
    Promise onError(PromiseTask onError);

    default Promise onError(PromiseTaskExecuteType promiseTaskExecuteType, BiConsumer<AtomicBoolean, Promise> fn) {
        return onError(new PromiseTask("onError", this, promiseTaskExecuteType, fn));
    }

    // Если в цепочку надо внедрить дополнительные задачи в runTime исполнения
    void addToHead(List<PromiseTask> append);

    Promise append(PromiseTask task);

    default Promise append(String index, PromiseTaskExecuteType promiseTaskExecuteType, BiConsumer<AtomicBoolean, Promise> fn) {
        return append(new PromiseTask(index, this, promiseTaskExecuteType, fn));
    }

    default <T extends Resource<?, ?, ?>> Promise appendWithResource(
            String index,
            Class<T> clasResource,
            TriConsumer<AtomicBoolean, Promise, T> procedure
    ) {
        return append(new PromiseTaskWithResource<>(
                index,
                this,
                procedure,
                clasResource
        ));
    }

    Promise then(PromiseTask task);

    default Promise then(String index, PromiseTaskExecuteType promiseTaskExecuteType, BiConsumer<AtomicBoolean, Promise> fn) {
        return then(new PromiseTask(index, this, promiseTaskExecuteType, fn));
    }

    Promise appendWait();

    PromiseTask getLastAppendedTask();

    List<TracePromise<String, Throwable>> getExceptionTrace();

    Collection<TracePromise<String, TraceTimer>> getTrace();

    String getLog();

    Promise extension(String index, PromiseExtension<?> promiseExtension);

    boolean isTerminated();

    boolean isException();

    void timeOut(String cause);

}
