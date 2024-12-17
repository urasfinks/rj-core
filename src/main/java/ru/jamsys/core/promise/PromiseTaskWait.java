package ru.jamsys.core.promise;

import lombok.ToString;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem
@ToString(onlyExplicitlyIncluded = true)
public class PromiseTaskWait extends PromiseTask {

    public PromiseTaskWait(Promise promise) {
        super(PromiseTaskExecuteType.WAIT.getNameCamel(), promise, PromiseTaskExecuteType.WAIT, null);
    }

    public PromiseTaskWait(String index, Promise promise) {
        super(index, promise, PromiseTaskExecuteType.WAIT, null);
    }

}
