package ru.jamsys.pool;

import org.springframework.context.ApplicationContext;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.rate.limit.RateLimit;
import ru.jamsys.rate.limit.RateLimitName;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoBalancerPool<T extends AbstractPool<?>> extends TaskStatisticHandler implements StatisticsCollector, KeepAlive {

    final protected Map<String, T> mapPool = new ConcurrentHashMap<>();

    final private RateLimitItem rateLimitMax;

    public AutoBalancerPool(ApplicationContext applicationContext) {
        RateLimit rateLimit = applicationContext.getBean(RateLimitManager.class).get(getClass(), null);
        rateLimitMax = rateLimit.get(RateLimitName.POOL_SIZE);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, mapPool, new String[0], (String key, T pool)
                -> result.addAll(pool.flushAndGetStatistic(parentTags, parentFields, isRun)));
        return result;
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        Map<String, Long> countResource = balancing(isRun, rateLimitMax.getMax());
        Util.riskModifierMap(isRun, mapPool, new String[0], (String key, T pool) -> {
            if (pool.isExpired()) {
                mapPool.remove(key);
                pool.shutdown();
                return;
            } else if (countResource.containsKey(key)) {
                pool.setMaxSlowRiseAndFastFall(countResource.get(key).intValue());
            } else {
                pool.setSumTime(0);
            }
            // 2024-03-20T13:42:08.002792 KeepAliveTask-1 add thread because: [KeepAliveTask] parkQueue: 0; resource: 1; remove: 0
            // На даём сами себя оживлять
            if (!pool.isAmI()) {
                pool.keepAlive();
            }
        });
    }
}
