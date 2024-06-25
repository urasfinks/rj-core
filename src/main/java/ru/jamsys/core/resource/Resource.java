package ru.jamsys.core.resource;

import ru.jamsys.core.resource.balancer.BalancerItem;

// C - Constructor
// A - ArgumentsExecute
// R - Result

public interface Resource<A, R> extends BalancerItem, ResourceCheckException {

    // Вызывается при создании экземпляра ресурса
    void constructor(NamespaceResourceConstructor constructor) throws Throwable;

    R execute(A arguments) throws Throwable;

    void close();

}
