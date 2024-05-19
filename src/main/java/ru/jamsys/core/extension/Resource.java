package ru.jamsys.core.extension;

import ru.jamsys.core.balancer.BalancerItem;

public interface Resource<A, R> extends BalancerItem {

    // Class<R> clsRes снимаем ответственность на unchecked
    // Агрументы сами преобразуем в то, что нам надо
    R execute(A arguments);

}
