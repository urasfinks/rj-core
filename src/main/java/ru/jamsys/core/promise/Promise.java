package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskConsumerThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskWithResourceConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.resource.PoolSettingsRegistry;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Цепочка обещаний

public interface Promise extends RepositoryMapClass<Object>, ExpirationMsImmutable, Correlation {

    Promise setDebug(boolean debug);

    boolean isDebug();

    String getIndex();

    void complete(@NonNull PromiseTask task, @NonNull Throwable exception);

    void complete(@Nullable PromiseTask task);

    void complete();

    // Синхронное ожидание выполнения Promise
    Promise await(long timeoutMs);

    // Синхронное ожидание выполнения Promise
    Promise await(long timeoutMs, int sleepIterationMs);

    // Запускаем цепочку задач от текущего потока
    Promise run();

    // Добавление задачи, которая выполнится после успешного завершения цепочки Promise
    Promise onComplete(PromiseTask onComplete);

    default Promise onComplete(PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> fn) {
        PromiseTask promiseTask = createTaskCompute("onCompleteTask", fn);
        promiseTask.setTerminated(true);
        return onComplete(promiseTask);
    }

    // Проверка, что добавлен обработчик ошибки
    boolean isSetErrorHandler();

    // Проверка, что добавлен обработчик успешного выполнения
    boolean isSetCompleteHandler();

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise
    Promise onError(PromiseTask onError);

    default Promise onError(PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> fn) {
        PromiseTask promiseTask = createTaskCompute("onErrorTask", fn);
        promiseTask.setTerminated(true);
        return onError(promiseTask);
    }

    // Если в цепочку надо внедрить дополнительные задачи в runTime исполнения
    void addToHead(List<PromiseTask> append);

    void addToHead(PromiseTask append);

    Promise append(PromiseTask task);

    default Promise append(List<PromiseTask> listPromiseTask) {
        listPromiseTask.forEach(this::append);
        return this;
    }

    default Promise append(String index, PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> fn) {
        return append(createTaskCompute(index, fn));
    }

    default <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            PromiseTaskWithResourceConsumerThrowing<PromiseTask, AtomicBoolean, Promise, T> procedure
    ) {
        return appendWithResource(index, classResource, "default", procedure);
    }

    default <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            String ns,
            PromiseTaskWithResourceConsumerThrowing<PromiseTask, AtomicBoolean, Promise, T> procedure
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
            PromiseTaskWithResourceConsumerThrowing<PromiseTask, AtomicBoolean, Promise, T> procedure
    ) {
        return thenWithResource(index, classResource, "default", procedure);
    }

    default <T extends Resource<?, ?>> Promise thenWithResource(
            String index,
            Class<T> classResource,
            String ns,
            PromiseTaskWithResourceConsumerThrowing<PromiseTask, AtomicBoolean, Promise, T> procedure
    ) {
        return then(new PromiseTaskWithResource<>(
                getIndex() + "." + index,
                this,
                procedure,
                App.get(PoolSettingsRegistry.class).get(classResource, ns)
        ));
    }

    default PromiseTask promiseToTask(String index, Promise externalPromise) {
        if (externalPromise == null) {
            return null;
        }
        this.setRepositoryMapClass(Promise.class, index, externalPromise);
        return createTaskExternal(index, (_, promiseTask, _) -> externalPromise
                .onError((_, _, promise) -> promiseTask.externalError(promise.getExceptionSource()))
                .onComplete((_, _, _) -> promiseTask.externalComplete())
                .run());
    }

    default Promise then(String index, Promise externalPromise) {
        if (externalPromise == null) {
            return this;
        }
        append(new PromiseTaskWait(index, this));
        append(promiseToTask(index, externalPromise));
        return this;
    }

    default Promise append(String index, Promise externalPromise) {
        if (externalPromise == null) {
            return this;
        }
        append(promiseToTask(index, externalPromise));
        return this;
    }

    default Promise then(PromiseTask task) {
        appendWait(task.getIndex()).append(task);
        return this;
    }

    default Promise then(String index, PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> fn) {
        return then(createTaskCompute(index, fn));
    }

    default Promise appendWait() {
        append(new PromiseTaskWait(this));
        return this;
    }

    default Promise appendWait(String key) {
        append(new PromiseTaskWait(key, this));
        return this;
    }

    PromiseTask getLastTask();

    List<Trace<String, Throwable>> getExceptionTrace();

    Collection<Trace<String, ?>> getTrace();

    String getLogString();

    boolean isRun();

    boolean isException();

    void timeOut(String cause);

    Throwable getExceptionSource();

    void setError(PromiseTask promiseTask, Throwable throwable);

    void setError(Throwable throwable);

    Promise setLog(boolean log);

    void skipAllStep(String cause);

    void goTo(String to);

    // Для удобства использования builder, что бы в цепочке можно было навешать promise плюшек)
    default Promise extension(ConsumerThrowing<Promise> cs) {
        try {
            cs.accept(this);
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
        return this;
    }

    Map<String, Object> getRepositoryMapWithoutDebug();

    default PromiseTask createTaskCompute(String index, PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> fn) {
        return new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.COMPUTE, fn);
    }

    default PromiseTask createTaskIo(String index, PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> fn) {
        return new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.IO, fn);
    }

    default PromiseTask createTaskExternal(String index, PromiseTaskConsumerThrowing<PromiseTask, AtomicBoolean, Promise> fn) {
        return new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.EXTERNAL_WAIT_COMPUTE, fn);
    }

}
