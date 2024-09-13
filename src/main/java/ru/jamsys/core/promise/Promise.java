package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.BiConsumerThrowing;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.functional.TriConsumerThrowing;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.resource.PoolSettingsRegistry;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Цепочка обещаний

public interface Promise extends RepositoryMapClass<Object>, ExpirationMsImmutable, Correlation {

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

    default Promise onComplete(BiConsumerThrowing<AtomicBoolean, Promise> fn) {
        PromiseTask promiseTask = new PromiseTask("onCompleteTask", this, PromiseTaskExecuteType.COMPUTE, fn);
        promiseTask.setTerminated(true);
        return onComplete(promiseTask);
    }

    // Проверка, что добавлен обработчик ошибки
    boolean isSetErrorHandler();

    // Проверка, что добавлен обработчик успешного выполнения
    boolean isSetCompleteHandler();

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise
    Promise onError(PromiseTask onError);

    default Promise onError(BiConsumerThrowing<AtomicBoolean, Promise> fn) {
        PromiseTask promiseTask = new PromiseTask("onErrorTask", this, PromiseTaskExecuteType.COMPUTE, fn);
        promiseTask.setTerminated(true);
        return onError(promiseTask);
    }

    // Если в цепочку надо внедрить дополнительные задачи в runTime исполнения
    void addToHead(List<PromiseTask> append);

    Promise append(PromiseTask task);

    default Promise append(String index, BiConsumerThrowing<AtomicBoolean, Promise> fn) {
        return append(new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.COMPUTE, fn));
    }

    default <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            TriConsumerThrowing<AtomicBoolean, Promise, T> procedure
    ) {
        return appendWithResource(index, classResource, "default", procedure);
    }

    default <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            String ns,
            TriConsumerThrowing<AtomicBoolean, Promise, T> procedure
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
            TriConsumerThrowing<AtomicBoolean, Promise, T> procedure
    ) {
        return thenWithResource(index, classResource, "default", procedure);
    }

    default <T extends Resource<?, ?>> Promise thenWithResource(
            String index,
            Class<T> classResource,
            String ns,
            TriConsumerThrowing<AtomicBoolean, Promise, T> procedure
    ) {
        return then(new PromiseTaskWithResource<>(
                getIndex() + "." + index,
                this,
                procedure,
                App.get(PoolSettingsRegistry.class).get(classResource, ns)
        ));
    }

    default Promise then(PromiseTask task) {
        appendWait(task.getIndex()).append(task);
        return this;
    }

    default Promise then(String index, BiConsumerThrowing<AtomicBoolean, Promise> fn) {
        return then(new PromiseTask(getIndex() + "." + index, this, PromiseTaskExecuteType.COMPUTE, fn));
    }

    default Promise appendWait() {
        append(new PromiseTask(PromiseTaskExecuteType.WAIT.getNameCamel(), this, PromiseTaskExecuteType.WAIT));
        return this;
    }

    default Promise appendWait(String key) {
        append(new PromiseTask(key, this, PromiseTaskExecuteType.WAIT));
        return this;
    }

    PromiseTask getLastTask();

    List<TracePromise<String, Throwable>> getExceptionTrace();

    Collection<TracePromise<String, ?>> getTrace();

    String getLogString();

    boolean isRun();

    boolean isException();

    void timeOut(String cause);

    Throwable getException();

    void setErrorInRunTask(Throwable throwable);

    Promise setLog(boolean log);

    void skipAllStep();

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

    Promise setDebug(boolean debug);

    boolean isDebug();

    Map<String, Object> getRepositoryMapWithoutDebug();

}
