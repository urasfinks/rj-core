package ru.jamsys.core.resource;

import ru.jamsys.core.balancer.BalancerItem;
import ru.jamsys.core.extension.LifeCycleInterface;

// C - Constructor
// A - ArgumentsExecute
// R - Result

public interface Resource<A, R> extends BalancerItem, ResourceCheckException, LifeCycleInterface {

    // Вызывается при создании экземпляра ресурса
    void setArguments(ResourceArguments resourceArguments) throws Throwable;

    R execute(A arguments) throws Throwable;

}
