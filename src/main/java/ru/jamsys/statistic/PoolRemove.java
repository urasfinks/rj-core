package ru.jamsys.statistic;

import org.springframework.context.ApplicationContext;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.pool.AbstractPool;
import ru.jamsys.rate.limit.RateLimitMax;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PoolRemove<T extends AbstractPool<?>> extends TimeWork implements StatisticsCollector, KeepAlive {

    final protected Map<String, T> mapPool = new ConcurrentHashMap<>();

    final private RateLimitMax rateLimitMax;

    public PoolRemove(ApplicationContext applicationContext) {
        this.rateLimitMax = applicationContext.getBean(RateLimitManager.class).get(getClass(), RateLimitMax.class, null);
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
        Map<String, Long> countThread = balancing(isRun, rateLimitMax.getMax());
        Util.riskModifierMap(isRun, mapPool, new String[0], (String indexTask, T threadPool) -> {
            if (threadPool.isExpired()) {
                mapPool.remove(indexTask);
                threadPool.shutdown();
                return;
            } else if (countThread.containsKey(indexTask)) {
                threadPool.setMaxSlowRiseAndFastFall(countThread.get(indexTask).intValue());
            } else {
                threadPool.setSumTime(0);
            }
            // 2024-03-20T13:42:08.002792 KeepAliveTask-1 add thread because: [KeepAliveTask] parkQueue: 0; resource: 1; remove: 0
            // На деём сами себя оживлять
            if (!threadPool.isAmI()) {
                threadPool.keepAlive();
            }
        });
    }
}
