package ru.jamsys.core.resource;

import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.pool.Valid;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// T - Argument
// R - Result

public interface Resource<T, R> extends ResourceCheckException, LifeCycleInterface, Valid, ExpirationMsMutable {

    // Вызывается при создании экземпляра ресурса
    void init(ResourceConfiguration resourceConfiguration) throws Throwable;

    R execute(T arguments) throws Throwable;

}
