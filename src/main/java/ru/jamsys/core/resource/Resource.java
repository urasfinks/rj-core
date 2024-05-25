package ru.jamsys.core.resource;

import ru.jamsys.core.resource.balancer.BalancerItem;

// C - Constructor
// A - ArgumentsExecute
// R - Result

public interface Resource<C, A, R> extends BalancerItem {

    void constructor(C constructor);

    // Class<R> clsRes снимаем ответственность на unchecked
    // Агрументы сами преобразуем в то, что нам надо
    R execute(A arguments);

    void close();

}
