package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.jdbc.ConnectionEnvelope;
import ru.jamsys.pool.JdbcPool;
import ru.jamsys.statistic.PoolRemove;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.task.JdbcRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@Component
@Lazy
public class JdbcManager extends PoolRemove<JdbcPool> implements KeepAliveComponent, StatisticsCollectorComponent {

    final private Map<String, JdbcPool> mapPool = new ConcurrentHashMap<>();

    public JdbcManager(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public ConnectionEnvelope get(JdbcRequest task) {
        String poolName = task.getPoolName();
        if (!mapPool.containsKey(poolName)) {
            mapPool.putIfAbsent(poolName, new JdbcPool(poolName, 0, 1));
        }
        JdbcPool jdbcPool = mapPool.get(poolName);
        jdbcPool.addResourceZeroPool();
        return jdbcPool.getResource();
    }

    public List<Map<String, Object>> execTask(JdbcRequest task) throws Exception {
        ConnectionEnvelope resource = get(task);
        if (resource == null) {
            throw new RuntimeException("Resource null");
        }
        TaskStatistic taskStatistic = getTaskStatistic(resource, task);
        List<Map<String, Object>> result = resource.exec(task);
        taskStatistic.finish();
        return result;
    }

}
