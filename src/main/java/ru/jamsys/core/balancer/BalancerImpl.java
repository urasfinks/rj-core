package ru.jamsys.core.balancer;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.balancer.algorithm.BalancerAlgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BalancerImpl<T> implements Balancer {

    @Getter
    List<BalancerItem> list = new ArrayList<>();

    List<BalancerItem> active = new CopyOnWriteArrayList<>();

    final BalancerAlgorithm balancerAlgorithm;

    public BalancerImpl(BalancerAlgorithm balancerAlgorithm) {
        this.balancerAlgorithm = balancerAlgorithm;
    }

    @Override
    public void set(BalancerItem item) {
        list.add(item);
    }

    @Override
    public BalancerItem get(@Nullable String index) {
        return this.balancerAlgorithm.get(index);
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        balancerAlgorithm.keepAlive(isThreadRun);
    }

}
