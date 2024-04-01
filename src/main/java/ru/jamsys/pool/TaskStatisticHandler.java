package ru.jamsys.pool;

import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.AvgMetricUnit;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.task.AbstractTask;
import ru.jamsys.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskStatisticHandler {

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
        Util.riskModifierCollection(isRun, queueTaskStatistics, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.isFinished()) {
                queueTaskStatistics.remove(taskStatistic);
            } else if (taskStatistic.getTimeExecuteMs() > taskStatistic.getTask().getMaxTimeExecute()) {
                queueTaskStatistics.remove(taskStatistic);
                taskStatistic.getResource().closeAndRemove();
            }
            String indexTask = taskStatistic.getTask().getIndex();
            if (!stat.containsKey(indexTask)) {
                stat.put(indexTask, new AvgMetric());
                mapPoolStatistic.put(indexTask, taskStatistic.getResource().getPool());
            }
            stat.get(indexTask).add(taskStatistic.getTimeExecuteMs());
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
