package ru.jamsys.core.component.promise.api;

import ru.jamsys.core.promise.Promise;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface PromiseApi<T> {

    void setPromise(Promise promise);

    Promise getPromise();

    void setIndex(String index);

    // Выполняется при инициализации
    PromiseApi<T> setup(Consumer<T> promiseApi);

    // Выполняется непосредственно перед стартом исполнителя
    PromiseApi<T> beforeExecute(Consumer<T> beforeExecute);

    void extend(Promise promise);

    Consumer<AtomicBoolean> getExecutor();

}
