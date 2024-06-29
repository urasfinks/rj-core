package ru.jamsys.core.balancer.algorithm;

import ru.jamsys.core.balancer.BalancerItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeastConnections implements BalancerAlgorithm {

    List<BalancerItem> list = new ArrayList<>();

    @Override
    public void update(List<BalancerItem> list) {
        this.list = list;
    }

    @Override
    public BalancerItem get(String index) {
        list.sort(Comparator.comparing(balancerItem -> balancerItem.getWeight(this)));
        return list.getFirst();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {

    }

}
