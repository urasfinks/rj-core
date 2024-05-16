package ru.jamsys.core.component.promise.api;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTaskType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Это всё находится в компонентах для того, что бы разделить зависимость конкретной реализации например http
// От реализации самого Promise
// Я сталкнулся с проблемой переноса ядра, когда в пакете http были зависимости на Promise
// У меня была задача по перенести реализации с тестами, а в итоге мне пришлось переносить всё сразу
// и исправлять сразу все ошибки а не по отдельности

@Setter
@Getter
public abstract class AbstractPromiseApi<T> implements PromiseApi<T> {

    protected PromiseTaskType promiseTaskType = PromiseTaskType.IO;

    private Promise promise;

    private String index;

    private String propertyResult = null;

    private Consumer<T> beforeExecute = null;

    public void setResult(Object result) {
        if (propertyResult != null) {
            promise.setProperty(propertyResult, result);
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
        this.promise = promise;
        promise.append(index, promiseTaskType, this::execute);
    }

    private void execute(AtomicBoolean isThreadRun) {
        if (beforeExecute != null) {
            beforeExecute.accept(get());
        }
        getExecutor().accept(isThreadRun);
    }

}
