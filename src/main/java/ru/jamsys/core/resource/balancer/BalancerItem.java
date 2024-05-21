package ru.jamsys.core.resource.balancer;

import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;

public interface BalancerItem {

    int getWeight(BalancerAlgorithm balancerAlgorithm); // 0 - эквивалентно отключению

}
