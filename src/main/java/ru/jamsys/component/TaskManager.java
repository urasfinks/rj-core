package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.broker.QueueElementEnvelope;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.ThreadPool;
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

    final private RateLimit rateLimit;

    final private Map<String, ThreadPool> mapPool = new ConcurrentHashMap<>();

    final private ConcurrentLinkedDeque<TaskStatistic> queueStatistics = new ConcurrentLinkedDeque<>();

    final private int maxCountIteration = 100; //Защита от бесконечной очереди

    public TaskManager(Broker broker, ExceptionHandler exceptionHandler, Dictionary dictionary, RateLimit rateLimit) {
        this.broker = broker;
        this.exceptionHandler = exceptionHandler;
        this.dictionary = dictionary;
        this.rateLimit = rateLimit;
    }

    public void addTask(Task task) throws Exception {
        String index = task.getIndex();
        if (!mapPool.containsKey(index)) {
            addPool(index);
        }
        broker.add(index, task);
        mapPool.get(index).wakeUp();
    }

    private void addPool(String index) {
        String rateLimitKey = getClass().getSimpleName() + "." + index;
        RateLimitItem rateLimitItem = rateLimit.get(rateLimitKey);
        if (!mapPool.containsKey(index)) {
            ThreadPool threadPool = new ThreadPool(
                    index,
                    0,
                    1,
                    60000,
                    (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                        int count = 0;
                        while (isWhile.get() && rateLimitItem.check()) {
                            // Защита от бесконечной очереди
                            // Предположим, что поменялось максимальное кол-во потоков и надо срезать потоки, а тут
                            // без остановки этот цикл молотит, не хорошо
                            if (count > maxCountIteration) {
                                return false;
                            }
                            QueueElementEnvelope<Task> queueElementEnvelope = broker.pollLast(index);
                            if (queueElementEnvelope == null) {
                                return false;
                            }
                            Task task = queueElementEnvelope.getElement();
                            @SuppressWarnings("unchecked")
                            Handler<Task> handler = dictionary.getTaskHandler().get(task.getClass());
                            if (handler != null) {
                                TaskStatistic taskStatistic = new TaskStatistic(threadEnvelope, task);
                                queueStatistics.add(taskStatistic);
                                try {
                                    handler.run(task, isWhile);
                                } catch (Exception e) {
                                    exceptionHandler.handler(e);
                                }
                                taskStatistic.finish();
                            } else {
                                exceptionHandler.handler(new RuntimeException("Not find TaskHandler for Task = " + task.getClass()));
                            }
                            count++;
                        }
                        return false;
                    }
            );
            threadPool.getListProcedureOnShutdown().add(() -> rateLimitItem.setActive(false));
            mapPool.put(index, threadPool);
            threadPool.run();
        }
    }

    public void removeInQueueStatistic(ThreadEnvelope threadEnvelope) {
        Util.riskModifierCollection(null, queueStatistics, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.getThreadEnvelope().equals(threadEnvelope)) {
                queueStatistics.remove(taskStatistic);
            }
        });
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        Map<String, ThreadPool> cloneMapPool = new HashMap<>(mapPool);
        Map<String, AvgMetric> stat = new HashMap<>();
        Util.riskModifierCollection(isRun, queueStatistics, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.isFinished()) {
                queueStatistics.remove(taskStatistic);
            }
            String index = taskStatistic.getTask().getIndex();
            if (!stat.containsKey(index)) {
                stat.put(index, new AvgMetric());
            }
            stat.get(index).add(taskStatistic.getTimeExecuteMs());
        });
        Map<String, Long> mapStat = new HashMap<>();
        for (String index : stat.keySet()) {
            AvgMetric avgMetric = stat.get(index);
            Map<String, Object> flush = avgMetric.flush("");
            mapStat.put(index, (Long) flush.get("Sum"));
        }
        Map<String, Long> calc = calc(mapStat, maxThread);
        for (String index : calc.keySet()) {
            ThreadPool obj = cloneMapPool.remove(index);
            obj.setMaxSlowRiseAndFastFall(calc.get(index).intValue());
            obj.setSumTime(mapStat.get(index));
        }
        // Очистка пустых пулов, что бы не мешались
        Util.riskModifierMap(isRun, cloneMapPool, new String[0], (String key, ThreadPool threadPool) -> {
            if (threadPool.isEmpty()) {
                cloneMapPool.remove(key);
                mapPool.remove(key);
                threadPool.shutdown();
            }
        });
        //Тем кто остался, но по ним за последние 3 секунды не было активности, выставляем пределы max = 1
        for (String index : cloneMapPool.keySet()) {
            mapPool.get(index).setMaxSlowRiseAndFastFall(1);
            mapPool.get(index).setSumTime(-1);
        }
        Util.riskModifierMap(isRun, mapPool, new String[0], (String key, ThreadPool obj) -> obj.keepAlive());
    }

    public static Map<String, Long> calc(Map<String, Long> map, int count) {
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
