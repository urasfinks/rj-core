package ru.jamsys.core.balancer;

import org.springframework.lang.Nullable;

import java.util.List;

public interface Balancer {

    BalancerItem get(@Nullable String index);

    List<BalancerItem> getList();

    void set(BalancerItem item);

}
