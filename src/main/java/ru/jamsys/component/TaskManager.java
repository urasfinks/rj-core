package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.pool.ThreadPool;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.AvgMetricUnit;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.Task;
import ru.jamsys.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TaskManager implements KeepAliveComponent, StatisticsCollectorComponent {

    final int maxThread = 500;

    final private Broker broker;

    final private ExceptionHandler exceptionHandler;

    final private Dictionary dictionary;

    final private Map<String, ThreadPool> mapPool = new ConcurrentHashMap<>();

    final private ConcurrentLinkedDeque<TaskStatistic> queueTaskStatistics = new ConcurrentLinkedDeque<>();

    public TaskManager(Broker broker, ExceptionHandler exceptionHandler, Dictionary dictionary) {
        this.broker = broker;
        this.exceptionHandler = exceptionHandler;
        this.dictionary = dictionary;
    }

    public void addTask(Task task) throws Exception {
        String taskIndex = task.getIndex();
        if (!mapPool.containsKey(taskIndex)) {
            ThreadPool threadPool = createThreadPool(taskIndex);
            mapPool.putIfAbsent(taskIndex, threadPool);
            threadPool.run();
        }
        if (task instanceof BrokerCollectible) {
            broker.add(taskIndex, (BrokerCollectible) task);
            mapPool.get(taskIndex).wakeUp();
        } else {
            exceptionHandler.handler(
                    new RuntimeException(task.getClass() + " not instanceof " + BrokerCollectible.class.getSimpleName())
            );
        }
    }

    private ThreadPool createThreadPool(String poolName) {
        return new ThreadPool(
                poolName,
                0,
                1,
                (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                    Task task = broker.pollLast(poolName);
                    if (task == null) {
                        return false;
                    }
                    @SuppressWarnings("unchecked")
                    Handler<Task> handler = dictionary.getTaskHandler().get(task.getClass());
                    if (handler != null) {
                        TaskStatistic taskStatistic = new TaskStatistic(threadEnvelope, task);
                        queueTaskStatistics.add(taskStatistic);
                        try {
                            handler.run(task, isWhile);
                        } catch (Exception e) {
                            exceptionHandler.handler(e);
                        }
                        taskStatistic.finish();
                    } else {
                        exceptionHandler.handler(new RuntimeException("Not find TaskHandler for Task = " + task.getClass()));
                    }
                    return false;
                }
        );
    }

    public void removeInQueueStatistic(ThreadEnvelope threadEnvelope) {
        Util.riskModifierCollection(null, queueTaskStatistics, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.getThreadEnvelope().equals(threadEnvelope)) {
                queueTaskStatistics.remove(taskStatistic);
            }
        });
    }

    private Map<String, Long> getTaskIndexSumTime(AtomicBoolean isRun) {
        Map<String, AvgMetric> stat = new HashMap<>();
        // Снимаем статистику задач, которые взяли в работу пулы потоков
        Util.riskModifierCollection(isRun, queueTaskStatistics, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.isFinished()) {
                queueTaskStatistics.remove(taskStatistic);
            }
            String indexTask = taskStatistic.getTask().getIndex();
            if (!stat.containsKey(indexTask)) {
                stat.put(indexTask, new AvgMetric());
            }
            stat.get(indexTask).add(taskStatistic.getTimeExecuteMs());
        });
        // Считаем агрегацию по собранным индексам
        Map<String, Long> mapStat = new HashMap<>();
        for (String index : stat.keySet()) {
            AvgMetric avgMetric = stat.get(index);
            Map<String, Object> flush = avgMetric.flush("");
            mapStat.put(index, (Long) flush.get(AvgMetricUnit.SUM.getName()));
        }
        return mapStat;
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        Map<String, Long> taskIndexSumTime = getTaskIndexSumTime(isRun);
        Map<String, Long> countThread = getCountThreadByTime(taskIndexSumTime, maxThread);
        Util.riskModifierMap(isRun, mapPool, new String[0], (String indexTask, ThreadPool threadPool) -> {
            if (threadPool.isExpired()) {
                mapPool.remove(indexTask);
                threadPool.shutdown();
                return;
            } else if (countThread.containsKey(indexTask)) {
                threadPool.setMaxSlowRiseAndFastFall(countThread.get(indexTask).intValue());
                threadPool.setSumTime(taskIndexSumTime.get(indexTask));
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

    public static Map<String, Long> getCountThreadByTime(Map<String, Long> map, int count) {
        Map<String, Long> normalizeMap = new LinkedHashMap<>();
        for (String index : map.keySet()) {
            Long aLong = map.get(index);
            normalizeMap.put(index, aLong < 1 ? 1 : aLong);
        }
        Map<String, Long> result = new HashMap<>();
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

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, mapPool, new String[0], (String key, ThreadPool threadPool)
                -> result.addAll(threadPool.flushAndGetStatistic(parentTags, parentFields, isRun)));
        return result;
    }

}
