package ru.jamsys.core.promise;

import lombok.Getter;
import ru.jamsys.core.extension.EnumName;

import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public enum PromiseTaskType implements EnumName {

    // Ждём когда все предыдущие запущенные задачи будут исполнены
    WAIT(PromiseImpl::isRunningTaskNotComplete, false),

    // Выполняем в том же потоке, на котором пошёл loop
    JOIN((_) -> false, true),

    //Запускаем эту задачу в VirtualThread
    IO((_) -> false, true),

    //Запускаем эту задачу в RealThread
    COMPUTE((_) -> false, true);

    private final Function<PromiseImpl, Boolean> fnIsRollback;

    @Getter
    private final boolean isRunnable;

    PromiseTaskType(Function<PromiseImpl, Boolean> fnIsRollback, boolean isRunnable) {
        this.fnIsRollback = fnIsRollback;
        this.isRunnable = isRunnable;
    }

    public boolean isRollback(PromiseImpl promiseImpl) {
        return fnIsRollback.apply(promiseImpl);
    }

}
