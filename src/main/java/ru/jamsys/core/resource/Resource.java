package ru.jamsys.core.resource;

import ru.jamsys.core.extension.LifeCycleInterface;

// C - Constructor
// A - ArgumentsExecute
// R - Result

public interface Resource<A, R> extends ResourceCheckException, LifeCycleInterface {

    // Вызывается при создании экземпляра ресурса
    void setArguments(ResourceArguments resourceArguments) throws Throwable;

    R execute(A arguments) throws Throwable;

    boolean isValid(); // Проверка, что ресурс валиден и готов к работе без всяких приколов

    // Это для поддержки интерфейса LifeCycleInterface
    // что бы для ресурсов не писать реализацию run
    default boolean isRun() {
        return isValid();
    }
}
