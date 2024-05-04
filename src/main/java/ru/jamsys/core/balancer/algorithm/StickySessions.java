package ru.jamsys.core.balancer.algorithm;

import org.springframework.lang.Nullable;
import ru.jamsys.core.balancer.BalancerItem;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class StickySessions implements BalancerAlgorithm {

    List<BalancerItem> list = new ArrayList<>();

    Map<String, TimeEnvelopeMs<BalancerItem>> map = new ConcurrentHashMap<>();

    @Override
    public void update(List<BalancerItem> list) {
        List<BalancerItem> diff = new ArrayList<>(list);
        for (BalancerItem balancerItem : list) {
            diff.remove(balancerItem);
            if (!this.list.contains(balancerItem)) {
                this.list.add(balancerItem);
            }
        }
        //diff содержит BalancerItem, которые вышли из балансировки
        Set<String> strings = map.keySet();
        for (BalancerItem balancerItem : diff) {
            remove(balancerItem);
        }
        this.list = list;
    }

    private void remove(BalancerItem balancerItem) {
        Util.riskModifierMap(null, map, new String[0], (String key, TimeEnvelopeMs<BalancerItem> timer) -> {
            if (timer.getValue().equals(balancerItem)) {
                map.remove(key);
            }
        });
    }

    @Override
    public BalancerItem get(@Nullable String index) {
        if (!map.containsKey(index)) {
            BalancerItem balancerItem = list.get(Util.stringToInt(index, 0, list.size()));
            TimeEnvelopeMs<BalancerItem> balancerItemTimeEnvelopeMs = new TimeEnvelopeMs<>(balancerItem);
            balancerItemTimeEnvelopeMs.setKeepAliveOnInactivityMin(5);
            map.put(index, balancerItemTimeEnvelopeMs);
        }
        TimeEnvelopeMs<BalancerItem> balancerItemTimeEnvelopeMs = map.get(index);
        balancerItemTimeEnvelopeMs.active();
        return balancerItemTimeEnvelopeMs.getValue();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        Util.riskModifierMap(isThreadRun, map, new String[0], (String key, TimeEnvelopeMs<BalancerItem> timeEnvelopeMs) -> {
            if (timeEnvelopeMs.isExpired()) {
                map.remove(key);
            }
        });
    }

}
