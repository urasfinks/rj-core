package ru.jamsys.core.promise;

public class PromiseTaskWait extends AbstractPromiseTask {

    public PromiseTaskWait(Promise promise) {
        super(PromiseTaskExecuteType.WAIT.getNameCamel(), promise, PromiseTaskExecuteType.WAIT, null);
    }

    public PromiseTaskWait(String index, Promise promise) {
        super(index, promise, PromiseTaskExecuteType.WAIT, null);
    }

}
