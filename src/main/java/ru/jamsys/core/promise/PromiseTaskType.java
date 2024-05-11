package ru.jamsys.core.promise;

import lombok.Getter;
import ru.jamsys.core.extension.EnumName;

@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public enum PromiseTaskType implements EnumName {

    // Ждём когда все предыдущие запущенные задачи будут исполнены
    WAIT(false),

    // Выполняем в том же потоке, на котором пошёл loop
    JOIN(true),

    //Запускаем эту задачу в VirtualThread
    IO(true),

    //Запускаем эту задачу в RealThread
    COMPUTE(true),

    // Асинхронная задача, complete будет вызван сторонними средствами
    ASYNC(true);

    private final boolean isRunnable;

    PromiseTaskType(boolean isRunnable) {
        this.isRunnable = isRunnable;
    }

}
