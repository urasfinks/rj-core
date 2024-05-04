package ru.jamsys.core.balancer;

public interface BalancerItem {

    boolean isActive();

    int getWeight();

    int getCountConnection();

}
