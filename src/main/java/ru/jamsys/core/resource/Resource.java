package ru.jamsys.core.resource;

import ru.jamsys.core.resource.balancer.BalancerItem;

import java.sql.SQLException;

// C - Constructor
// A - ArgumentsExecute
// R - Result

public interface Resource<C, A, R> extends BalancerItem {

    // Вызывается при создании экземпляра ресурса
    void constructor(C constructor) throws Throwable;

    R execute(A arguments) throws Throwable;

    void close();

}
