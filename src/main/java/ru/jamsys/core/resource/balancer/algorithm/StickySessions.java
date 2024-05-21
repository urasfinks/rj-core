package ru.jamsys.core.resource.balancer.algorithm;

import org.springframework.lang.Nullable;
import ru.jamsys.core.resource.balancer.BalancerItem;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableEnvelope;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class StickySessions implements BalancerAlgorithm {

    List<BalancerItem> list = new ArrayList<>();

    Map<String, ExpirationMsMutableEnvelope<BalancerItem>> map = new ConcurrentHashMap<>();

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
        UtilRisc.forEach(null, map, (String key, ExpirationMsMutableEnvelope<BalancerItem> timer) -> {
            if (timer.getValue().equals(balancerItem)) {
                map.remove(key);
            }
        });
    }

    @Override
    public BalancerItem get(@Nullable String index) {
        map.computeIfAbsent(index, _ -> {
            BalancerItem balancerItem = list.get(Util.stringToInt(index, 0, list.size()));
            ExpirationMsMutableEnvelope<BalancerItem> balancerItemExpirationMsMutableEnvelope = new ExpirationMsMutableEnvelope<>(balancerItem);
            balancerItemExpirationMsMutableEnvelope.setKeepAliveOnInactivityMin(5);
            return balancerItemExpirationMsMutableEnvelope;
        });
        ExpirationMsMutableEnvelope<BalancerItem> balancerItemExpirationMsMutableEnvelope = map.get(index);
        balancerItemExpirationMsMutableEnvelope.active();
        return balancerItemExpirationMsMutableEnvelope.getValue();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        UtilRisc.forEach(isThreadRun, map, (String key, ExpirationMsMutableEnvelope<BalancerItem> expiredMsMutableEnvelope) -> {
            if (expiredMsMutableEnvelope.isExpired()) {
                map.remove(key);
            }
        });
    }

}
