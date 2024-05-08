package ru.jamsys.core.balancer.algorithm;

import org.springframework.lang.Nullable;
import ru.jamsys.core.balancer.BalancerItem;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;
import ru.jamsys.core.util.Util;
import ru.jamsys.core.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class StickySessions implements BalancerAlgorithm {

    List<BalancerItem> list = new ArrayList<>();

    Map<String, ExpiredMsMutableEnvelope<BalancerItem>> map = new ConcurrentHashMap<>();

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
        UtilRisc.forEach(null, map, (String key, ExpiredMsMutableEnvelope<BalancerItem> timer) -> {
            if (timer.getValue().equals(balancerItem)) {
                map.remove(key);
            }
        });
    }

    @Override
    public BalancerItem get(@Nullable String index) {
        map.computeIfAbsent(index, s -> {
            BalancerItem balancerItem = list.get(Util.stringToInt(index, 0, list.size()));
            ExpiredMsMutableEnvelope<BalancerItem> balancerItemExpiredMsMutableEnvelope = new ExpiredMsMutableEnvelope<>(balancerItem);
            balancerItemExpiredMsMutableEnvelope.setKeepAliveOnInactivityMin(5);
            return balancerItemExpiredMsMutableEnvelope;
        });
        ExpiredMsMutableEnvelope<BalancerItem> balancerItemExpiredMsMutableEnvelope = map.get(index);
        balancerItemExpiredMsMutableEnvelope.active();
        return balancerItemExpiredMsMutableEnvelope.getValue();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        UtilRisc.forEach(isThreadRun, map, (String key, ExpiredMsMutableEnvelope<BalancerItem> expiredMsMutableEnvelope) -> {
            if (expiredMsMutableEnvelope.isExpired()) {
                map.remove(key);
            }
        });
    }

}
