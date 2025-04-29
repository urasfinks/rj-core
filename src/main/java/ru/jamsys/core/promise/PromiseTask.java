package ru.jamsys.core.promise;

import lombok.Getter;
import ru.jamsys.core.extension.functional.PromiseTaskConsumerThrowing;

import java.util.concurrent.atomic.AtomicBoolean;


@Getter
public class PromiseTask extends AbstractPromiseTask {

    public PromiseTask(
            String index,
            Promise promise,
            PromiseTaskExecuteType type,
            PromiseTaskConsumerThrowing<AbstractPromiseTask, AtomicBoolean, Promise> procedure
    ) {
        super(index, promise, type, procedure);
    }

}
