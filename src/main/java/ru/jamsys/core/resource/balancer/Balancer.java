package ru.jamsys.core.resource.balancer;

import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.KeepAlive;

import java.util.List;

public interface Balancer extends KeepAlive {

    BalancerItem get(@Nullable String index);

    List<BalancerItem> getList();

    void set(BalancerItem item);

}
