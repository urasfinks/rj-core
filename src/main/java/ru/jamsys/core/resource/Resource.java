package ru.jamsys.core.resource;

import ru.jamsys.core.resource.balancer.BalancerItem;

public interface Resource<A, R> extends BalancerItem {

    // Class<R> clsRes снимаем ответственность на unchecked
    // Агрументы сами преобразуем в то, что нам надо
    R execute(A arguments);

}
