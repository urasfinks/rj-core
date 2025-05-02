package ru.jamsys.core.promise;

import lombok.ToString;

@ToString
public class PromiseTaskWait extends AbstractPromiseTask {

    public PromiseTaskWait(Promise promise) {
        super(PromiseTaskExecuteType.WAIT.getNameCamel(), promise, PromiseTaskExecuteType.WAIT, null);
    }

    public PromiseTaskWait(String index, Promise promise) {
        super(index, promise, PromiseTaskExecuteType.WAIT, null);
    }

}
