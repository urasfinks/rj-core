package ru.jamsys.core.resource;

import ru.jamsys.core.balancer.BalancerItem;
import ru.jamsys.core.extension.LifeCycleInterface;

// C - Constructor
// A - ArgumentsExecute
// R - Result

public interface Resource<A, R> extends BalancerItem, ResourceCheckException, LifeCycleInterface {

    // Вызывается при создании экземпляра ресурса
    void constructor(NamespaceResourceConstructor constructor) throws Throwable;

    R execute(A arguments) throws Throwable;

}
