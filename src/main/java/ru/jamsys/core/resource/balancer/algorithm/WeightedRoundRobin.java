package ru.jamsys.core.resource.balancer.algorithm;

import org.springframework.lang.Nullable;
import ru.jamsys.core.resource.balancer.BalancerItem;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class WeightedRoundRobin implements BalancerAlgorithm {

    TreeMap<Double, BalancerItem> map = new TreeMap<>();
    private double sumWeight;

    @Override
    public void update(List<BalancerItem> list) {
        TreeMap<Double, BalancerItem> pool = new TreeMap<>();
        double totalWeight = 0;
        for (BalancerItem resource : list.toArray(new BalancerItem[0])) {
            int weight = resource.getWeight(this);
            if (weight > 0) {
                totalWeight += weight;
                pool.put(totalWeight, resource);
            }
        }
        this.map = pool;
        this.sumWeight = totalWeight;
    }

    @Override
    public BalancerItem get(@Nullable String index) {
        if (sumWeight == 0) {
            return null;
        }
        return map.ceilingEntry(ThreadLocalRandom.current().nextDouble() * sumWeight).getValue();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {

    }

}
