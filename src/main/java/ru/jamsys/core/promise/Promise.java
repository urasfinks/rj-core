package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.functional.iface.TriConsumer;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.resource.PoolSettingsRegistry;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutable;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

// Цепочка обещаний

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
        PromiseTask promiseTask = new PromiseTask("onCompleteTask", this, App.getUsualExecutor(), fn);
        promiseTask.setTerminated(true);
        return onComplete(promiseTask);
    }

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise
    Promise onError(PromiseTask onError);

    default Promise onError(BiConsumer<AtomicBoolean, Promise> fn) {
        PromiseTask promiseTask = new PromiseTask("onErrorTask", this, App.getUsualExecutor(), fn);
        promiseTask.setTerminated(true);
        return onError(promiseTask);
    }

    // Если в цепочку надо внедрить дополнительные задачи в runTime исполнения
    void addToHead(List<PromiseTask> append);

    Promise append(PromiseTask task);

    default Promise append(String index, BiConsumer<AtomicBoolean, Promise> fn) {
        return append(new PromiseTask(getIndex() + "." + index, this, App.getUsualExecutor(), fn));
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

    default <T extends Resource<?, ?>> Promise thenWithResource(
            String index,
            Class<T> classResource,
            TriConsumer<AtomicBoolean, Promise, T> procedure
    ) {
        return thenWithResource(index, classResource, "default", procedure);
    }

    default <T extends Resource<?, ?>> Promise thenWithResource(
            String index,
            Class<T> classResource,
            String ns,
            TriConsumer<AtomicBoolean, Promise, T> procedure
    ) {
        return then(new PromiseTaskWithResource<>(
                getIndex() + "." + index,
                this,
                procedure,
                App.get(PoolSettingsRegistry.class).get(classResource, ns)
        ));
    }

    default Promise then(PromiseTask task) {
        appendWait().append(task);
        return this;
    }

    default Promise then(String index, BiConsumer<AtomicBoolean, Promise> fn) {
        return then(new PromiseTask(getIndex() + "." + index, this, App.getUsualExecutor(), fn));
    }

    default Promise appendWait() {
        append(new PromiseTask(PromiseTaskExecuteType.WAIT.getNameCamel(), this, PromiseTaskExecuteType.WAIT));
        return this;
    }

    PromiseTask getLastTask();

    List<TracePromise<String, Throwable>> getExceptionTrace();

    Collection<TracePromise<String, TimerNanoEnvelope<String>>> getTrace();

    String getLogString();

    boolean isRun();

    boolean isException();

    void timeOut(String cause);

    Throwable getException();

    void setErrorInRunTask(Throwable throwable);

    Promise setLog(boolean log);

    void skipAllStep();

    void goTo(String to);

}
