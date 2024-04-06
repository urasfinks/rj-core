package ru.jamsys.pool;

import org.springframework.context.ApplicationContext;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.component.general.AbstractComponent;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.rate.limit.RateLimit;
import ru.jamsys.rate.limit.RateLimitName;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.AvgMetricUnit;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.AbstractTask;
import ru.jamsys.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AutoBalancerPools<
        MO extends
                AbstractPool<MOI>
                & Closable
                & StatisticsCollector
                & RunnableInterface,
        MOI extends AbstractPoolResource<MOI>
        >
        extends AbstractComponent<MO> {

    final private RateLimitItem rateLimitMax;

    public AutoBalancerPools(ApplicationContext applicationContext) {
        RateLimit rateLimit = applicationContext.getBean(RateLimitManager.class).get(getClass(), null);
        rateLimitMax = rateLimit.get(RateLimitName.POOL_SIZE);
    }

    @Override
    public void keepAlive(ThreadEnvelope threadEnvelope) {
        Map<String, Long> countResource = balancing(threadEnvelope.getIsWhile(), rateLimitMax.getMax());
        Util.riskModifierMap(threadEnvelope.getIsWhile(), map, new String[0], (String key, MO pool) -> {
            if (pool.isExpired()) {
                map.remove(key);
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
                pool.keepAlive(threadEnvelope);
            }
        });
    }

    final private ConcurrentLinkedDeque<TaskStatistic> queueTaskStatistics = new ConcurrentLinkedDeque<>();

    public TaskStatistic getTaskStatistic(AbstractPoolResource<?> abstractPoolResource, AbstractTask task) {
        TaskStatistic taskStatistic = new TaskStatistic(abstractPoolResource, task);
        queueTaskStatistics.add(taskStatistic);
        return taskStatistic;
    }

    // Вызывается когда произошла неизбежность thread.stop()
    public void forceRemove(AbstractPoolResource<?> abstractPoolResource) {
        Util.riskModifierCollection(null, queueTaskStatistics, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.getResource().equals(abstractPoolResource)) {
                queueTaskStatistics.remove(taskStatistic);
            }
        });
    }

    public Map<String, Long> balancing(AtomicBoolean isRun, long count) {
        Map<String, AvgMetric> stat = new HashMap<>();
        // Снимаем статистику задач, которые взял в работу пул
        Map<String, Pool<?>> mapPoolStatistic = new HashMap<>();
        long curTime = System.currentTimeMillis();
        Util.riskModifierCollection(isRun, queueTaskStatistics, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.isStop()) {
                queueTaskStatistics.remove(taskStatistic);
            } else if (taskStatistic.isExpired(curTime)) {
                queueTaskStatistics.remove(taskStatistic);
                taskStatistic.getResource().closeAndRemove();
            }
            String indexTask = taskStatistic.getTask().getIndex();
            if (!stat.containsKey(indexTask)) {
                stat.put(indexTask, new AvgMetric());
                mapPoolStatistic.put(indexTask, taskStatistic.getResource().getPool());
            }
            stat.get(indexTask).add(taskStatistic.getOffsetLastActivityMs(curTime));
        });
        // Считаем агрегацию по собранным индексам
        Map<String, Long> sumTimeMap = new HashMap<>();
        for (String index : stat.keySet()) {
            AvgMetric avgMetric = stat.get(index);
            Map<String, Object> flush = avgMetric.flush("");
            Long sumTime = (Long) flush.get(AvgMetricUnit.SUM.getNameCache());
            mapPoolStatistic.get(index).setSumTime(sumTime);
            sumTimeMap.put(index, sumTime);
        }
        return getMaxCountResourceByTime(sumTimeMap, count);
    }

    public static Map<String, Long> getMaxCountResourceByTime(Map<String, Long> map, long count) {
        Map<String, Long> result = new HashMap<>();
        if (map.isEmpty()) {
            return result;
        }
        Map<String, Long> normalizeMap = new LinkedHashMap<>();
        for (String index : map.keySet()) {
            Long aLong = map.get(index);
            normalizeMap.put(index, aLong < 1 ? 1 : aLong);
        }
        BigDecimal maxCount = new BigDecimal(count);
        BigDecimal maxPrc = new BigDecimal(100);

        long sumMax = 0;
        for (String index : normalizeMap.keySet()) {
            sumMax += normalizeMap.get(index);
        }
        if (sumMax == 0) {
            BigDecimal sum = maxCount.divide(new BigDecimal(normalizeMap.size()), 5, RoundingMode.HALF_UP);
            for (String index : normalizeMap.keySet()) {
                result.put(index, sum.longValue());
            }
        } else {
            BigDecimal sum = new BigDecimal(sumMax);
            BigDecimal sumReverse = new BigDecimal(0);
            Map<String, BigDecimal> prc = new HashMap<>();
            BigDecimal one = new BigDecimal(1);
            for (String index : normalizeMap.keySet()) {
                BigDecimal currentPercent = new BigDecimal(normalizeMap.get(index))
                        .multiply(maxPrc)
                        .divide(sum, 5, RoundingMode.HALF_UP);
                BigDecimal reversePercent = one.divide(currentPercent, 5, RoundingMode.HALF_UP);
                prc.put(index, reversePercent);
                sumReverse = sumReverse.add(reversePercent);
            }
            for (String index : prc.keySet()) {
                long res = prc
                        .get(index)
                        .multiply(maxPrc)
                        .divide(sumReverse, 5, RoundingMode.HALF_UP)
                        .multiply(maxCount)
                        .divide(maxPrc, 5, RoundingMode.HALF_UP)
                        .longValue();

                result.put(index, res < 1 ? 1 : res);
            }
        }
        return result;
    }

}
