package ru.jamsys.core.promise.resource.extension;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Это всё находится в компонентах для того, что бы разделить зависимость конкретной реализации например http
// От реализации самого Promise
// Я сталкнулся с проблемой переноса ядра, когда в пакете http были зависимости на Promise
// У меня была задача по перенести реализации с тестами, а в итоге мне пришлось переносить всё сразу
// и исправлять сразу все ошибки а не по отдельности

@Setter
@Getter
public abstract class AbstractPromiseExtension<T> implements PromiseExtension<T> {

    protected PromiseTaskExecuteType promiseTaskExecuteType = PromiseTaskExecuteType.IO;

    private String index;

    private String propertyResult = null;

    private Consumer<T> beforeExecute = null;

    @Override
    public PromiseExtension<T> beforeExecute(Consumer<T> beforeExecute) {
        this.beforeExecute = beforeExecute;
        return this;
    }

    T get() {
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return t;
    }

    @Override
    public PromiseExtension<T> setup(Consumer<T> promiseExtension) {
        promiseExtension.accept(get());
        return this;
    }

    @Override
    public void extend(Promise promise) {
        promise.append(index, promiseTaskExecuteType, this::execute);
    }

    private void execute(AtomicBoolean isThreadRun, Promise promise1) {
        if (beforeExecute != null) {
            beforeExecute.accept(get());
        }
        getExecutor().accept(isThreadRun);
    }

}
