package ru.jamsys.component;

import lombok.Getter;
import org.springframework.stereotype.Component;
import ru.jamsys.KeepAlive;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.ThreadPool;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.RollbackThreadEnvelopeInParkTask;
import ru.jamsys.thread.task.Task;
import ru.jamsys.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TaskManager implements KeepAlive {

    final int maxThread = 500;

    final private Broker broker;

    final private ExceptionHandler exceptionHandler;

    final private Dictionary dictionary;

    final private Map<String, Index> mapPool = new ConcurrentHashMap<>();

    final private ConcurrentLinkedDeque<TaskStatistic> queue = new ConcurrentLinkedDeque<>();

    public TaskManager(Broker broker, ExceptionHandler exceptionHandler, Dictionary dictionary) {
        this.broker = broker;
        this.exceptionHandler = exceptionHandler;
        this.dictionary = dictionary;
    }

    public void add(Task task) {
        String index = task.getIndex();
        if (!mapPool.containsKey(index)) {
            addPool(index, task);
        }
        try {
            broker.get(index).add(task);
            mapPool.get(index).getThreadPool().wakeUp();
        } catch (Exception e) {
            exceptionHandler.handler(e);
        }
    }

    private void addPool(String name, Task ownerTask) {
        if (!mapPool.containsKey(name)) {
            int minThread = ownerTask instanceof RollbackThreadEnvelopeInParkTask ? 1 : 0;
            ThreadPool threadPool = new ThreadPool(
                    name,
                    minThread,
                    1,
                    60000,
                    (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                        while (isWhile.get()) {
                            Task task = broker.pollLast(name);
                            if (task == null) {
                                return false;
                            }
                            @SuppressWarnings("unchecked")
                            Handler<Task> handler = dictionary.getTaskHandler().get(task.getClass());
                            if (handler != null) {
                                TaskStatistic taskStatistic = new TaskStatistic(threadEnvelope, task);
                                queue.add(taskStatistic);
                                try {
                                    handler.run(task, isWhile);
                                } catch (Exception e) {
                                    exceptionHandler.handler(e);
                                }
                                taskStatistic.finish();
                            } else {
                                exceptionHandler.handler(new RuntimeException("Not find TaskHandler for Task = " + task.getClass()));
                            }
                        }
                        return false;
                    }
            );
            mapPool.put(name, new Index(threadPool));
            threadPool.run();
        }
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        Util.logConsole("-------------------");
        Map<String, Index> cloneMapPool = new HashMap<>(mapPool);
        Map<String, AvgMetric> stat = new HashMap<>();
        Util.riskModifierCollection(isRun, queue, new TaskStatistic[0], (TaskStatistic taskStatistic) -> {
            if (taskStatistic.isFinished()) {
                queue.remove(taskStatistic);
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
        Util.logConsole("stat: " + mapStat);
        Map<String, Long> calc = calc(mapStat, maxThread);
        Util.logConsole("calc: " + calc);
        for (String index : calc.keySet()) {
            Index obj = cloneMapPool.remove(index);
            //Util.logConsole(index + "set max = " + calc.get(index).intValue());
            obj.getThreadPool().setMax(calc.get(index).intValue());
        }
        // Очистка пустых пулов, что бы не мешались
        Util.riskModifierMap(isRun, cloneMapPool, new String[0], (String key, Index obj) -> {
            if (obj.getThreadPool().isEmpty()) {
                cloneMapPool.remove(key);
                mapPool.remove(key);
            }
        });
        //Тем кто остался, но по ним за последние 3 секунды не было активности, выставляем пределы max = 1
        for (String index : cloneMapPool.keySet()) {
            //Util.logConsole(index + " set max = 1");
            mapPool.get(index).getThreadPool().setMax(1);
        }
        Util.riskModifierMap(isRun, mapPool, new String[0], (String key, Index obj) -> {
            obj.getThreadPool().keepAlive();
        });
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
        BigDecimal midPercent = maxPrc.divide(new BigDecimal(normalizeMap.size()), 5, RoundingMode.HALF_UP);
        if (sumMax == 0) {
            BigDecimal sum = maxCount.divide(new BigDecimal(normalizeMap.size()), 5, RoundingMode.HALF_UP);
            for (String index : normalizeMap.keySet()) {
                result.put(index, sum.longValue());
            }
        } else {
            BigDecimal sum = new BigDecimal(sumMax);
            for (String index : normalizeMap.keySet()) {
                BigDecimal currentPercent = new BigDecimal(normalizeMap.get(index))
                        .multiply(maxPrc)
                        .divide(sum, 5, RoundingMode.HALF_UP);
                Long countThread = midPercent.subtract(currentPercent)
                        .add(midPercent)
                        .multiply(maxCount)
                        .divide(maxPrc, 0, RoundingMode.HALF_UP)
                        .longValue();
                result.put(index, countThread);
                //result.put(index, 0L);
            }
        }

        return result;
    }

    @Getter
    public static class Index {

        final ThreadPool threadPool;

        final AtomicLong sumTime = new AtomicLong(0);

        public Index(ThreadPool threadPool) {
            this.threadPool = threadPool;
        }

    }
}
