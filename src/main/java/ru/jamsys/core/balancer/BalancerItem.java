package ru.jamsys.core.balancer;

import ru.jamsys.core.balancer.algorithm.BalancerAlgorithm;

public interface BalancerItem {

    int getWeight(BalancerAlgorithm balancerAlgorithm); // 0 - эквивалентно отключению

}
