package ru.jamsys.core.resource;

import ru.jamsys.core.resource.balancer.BalancerItem;

// C - Constructor
// A - ArgumentsExecute
// R - Result

public interface Resource<C, A, R> extends BalancerItem {

    // Вызывается при создании экземпляра ресурса
    void constructor(C constructor);

    R execute(A arguments);

    void close();

}
