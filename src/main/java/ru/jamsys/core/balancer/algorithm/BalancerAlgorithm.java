package ru.jamsys.core.balancer.algorithm;

import org.springframework.lang.Nullable;
import ru.jamsys.core.balancer.BalancerItem;
import ru.jamsys.core.extension.KeepAlive;

import java.util.List;

public interface BalancerAlgorithm extends KeepAlive {

    void update(List<BalancerItem> list);

    BalancerItem get(@Nullable String index);

}
