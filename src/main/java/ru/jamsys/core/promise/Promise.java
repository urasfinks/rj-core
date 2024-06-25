package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.TriConsumer;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.resource.PoolSettingsRegistry;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutable;
import ru.jamsys.core.statistic.timer.Timer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

// Цепочка обещаний
// TODO: сделать finish() что skipAllStep
// TODO: сделать jump(indexTask) что вычеркнуть из списка все задачи до индекса таски

public interface Promise extends Property<String, Object>, ExpirationMsImmutable, Correlation {

    String getIndex();

    void complete(@NonNull PromiseTask task, @NonNull Throwable exception);

    void complete(@Nullable PromiseTask task);

    void complete();

    // Синхронное ожидание выполнения Promise
    void await(long timeoutMs);

    // Запускаем цепочку задач от текущего потока
    Promise run();

    // Добавление задачи, которая выполнится после успешного завершения цепочки Promise
    Promise onComplete(PromiseTask onComplete);

    default Promise onComplete(BiConsumer<AtomicBoolean, Promise> fn) {
        return onComplete(new PromiseTask(getIndex() + ".onComplete", this, PromiseTaskExecuteType.JOIN, fn));
    }

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise
    Promise onError(PromiseTask onError);

    default Promise onError(BiConsumer<AtomicBoolean, Promise> fn) {
        return onError(new PromiseTask(getIndex() + ".onError", this, PromiseTaskExecuteType.JOIN, fn));
    }

    // Если в цепочку надо внедрить дополнительные задачи в runTime исполнения
    void addToHead(List<PromiseTask> append);

    Promise append(PromiseTask task);

    default Promise append(String index, BiConsumer<AtomicBoolean, Promise> fn) {
        //TODO: IO -> COMPUTED
        return append(new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.IO, fn));
    }

    default <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            TriConsumer<AtomicBoolean, Promise, T> procedure
    ) {
        return appendWithResource(index, classResource, "default", procedure);
    }

    default <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            String ns,
            TriConsumer<AtomicBoolean, Promise, T> procedure
    ) {
        return append(new PromiseTaskWithResource<>(
                getIndex() + "." + index,
                this,
                procedure,
                App.get(PoolSettingsRegistry.class).get(classResource, ns)
        ));
    }

    Promise then(PromiseTask task);

    default Promise then(String index, BiConsumer<AtomicBoolean, Promise> fn) {
        //TODO: IO -> COMPUTED
        return then(new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.IO, fn));
    }

    default Promise join(String index, BiConsumer<AtomicBoolean, Promise> fn) {
        return append(new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.JOIN, fn));
    }

    Promise appendWait();

    PromiseTask getLastAppendedTask();

    List<TracePromise<String, Throwable>> getExceptionTrace();

    Collection<TracePromise<String, Timer>> getTrace();

    String getLog();

    boolean isTerminated();

    boolean isException();

    void timeOut(String cause);

    Throwable getException();

}
