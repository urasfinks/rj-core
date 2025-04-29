package ru.jamsys.core.balancer.algorithm;

import org.springframework.lang.Nullable;
import ru.jamsys.core.balancer.BalancerItem;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: нет использования helper

public interface BalancerAlgorithm {

    void update(List<BalancerItem> list);

    BalancerItem get(@Nullable String index);

    void helper(AtomicBoolean threadRun);

}
