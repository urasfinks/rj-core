package ru.jamsys.core.extension;

import ru.jamsys.core.promise.PromiseTask;

public interface ResourcePromiseTask<R, A> extends Resource<R, A> {

    void executeAsync(A arguments, PromiseTask promiseTask);

}
