package ru.jamsys.core.promise.resource.extension;

import ru.jamsys.core.promise.Promise;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface PromiseExtension<T> {

    void setPromise(Promise promise);

    Promise getPromise();

    void setIndex(String index);

    // Выполняется при инициализации
    PromiseExtension<T> setup(Consumer<T> promiseApi);

    // Выполняется непосредственно перед стартом исполнителя
    PromiseExtension<T> beforeExecute(Consumer<T> beforeExecute);

    void extend(Promise promise);

    Consumer<AtomicBoolean> getExecutor();

}
