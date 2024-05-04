package ru.jamsys.core.component.promise.api;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTaskType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Setter
@Getter
public abstract class AbstractPromiseApi<T> implements PromiseApi<T> {

    private PromiseTaskType promiseTaskType = PromiseTaskType.IO;

    private Promise promise;

    private String index;

    private String propertyResult = null;

    private Consumer<T> beforeExecute = null;

    public void setResult(Object result) {
        if (propertyResult != null) {
            promise.getProperty().put(propertyResult, result);
        }
    }

    @Override
    public PromiseApi<T> beforeExecute(Consumer<T> beforeExecute) {
        this.beforeExecute = beforeExecute;
        return this;
    }

    T get() {
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return t;
    }

    @Override
    public PromiseApi<T> setup(Consumer<T> promiseApi) {
        promiseApi.accept(get());
        return this;
    }

    @Override
    public void extend(Promise promise) {
        promise.append(index, promiseTaskType, this::execute);
    }

    private void execute(AtomicBoolean isThreadRun) {
        if (beforeExecute != null) {
            beforeExecute.accept(get());
        }
        getExecutor().accept(isThreadRun);
    }

}
