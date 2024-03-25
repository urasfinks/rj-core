package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.jdbc.ConnectionEnvelope;
import ru.jamsys.pool.JdbcPool;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.task.JdbcRequest;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class JdbcManager implements KeepAliveComponent, StatisticsCollectorComponent {

    final private Map<String, JdbcPool> mapPool = new ConcurrentHashMap<>();

    final private ConcurrentLinkedDeque<TaskStatistic> queueTaskStatistics = new ConcurrentLinkedDeque<>();

    public ConnectionEnvelope get(JdbcRequest task) {
        String poolName = task.getPoolName();
        if (!mapPool.containsKey(poolName)) {
            mapPool.putIfAbsent(poolName, new JdbcPool(poolName, 0, 1));
        }
        JdbcPool jdbcPool = mapPool.get(poolName);
        jdbcPool.addResourceZeroPool();
        return jdbcPool.getResource();
    }

    public List<Map<String, Object>> exec(JdbcRequest task) throws Exception {
        ConnectionEnvelope resource = get(task);
        if (resource == null) {
            throw new RuntimeException("Resource null");
        }
        TaskStatistic taskStatistic = new TaskStatistic(resource, task);
        queueTaskStatistics.add(taskStatistic);
        List<Map<String, Object>> result = resource.exec(task);
        taskStatistic.finish();
        return result;
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        Util.riskModifierMap(isRun, mapPool, new String[0], (String indexPool, JdbcPool jdbcPool) -> {
            if (jdbcPool.isExpired()) {
                mapPool.remove(indexPool);
                jdbcPool.shutdown();
                return;
            }
            jdbcPool.keepAlive();
        });
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, mapPool, new String[0], (String key, JdbcPool jdbcPool)
                -> result.addAll(jdbcPool.flushAndGetStatistic(parentTags, parentFields, isRun)));
        return result;
    }
}
