package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.jdbc.ConnectionEnvelope;
import ru.jamsys.pool.AutoBalancerPool;
import ru.jamsys.pool.JdbcPool;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.JdbcRequest;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Component
@Lazy
public class JdbcManager extends AutoBalancerPool<JdbcPool> implements KeepAliveComponent, StatisticsCollectorComponent {

    public JdbcManager(ApplicationContext applicationContext, Broker broker) {
        super(applicationContext);
    }

    public List<Map<String, Object>> execTask(JdbcRequest task, ThreadEnvelope threadEnvelope) throws Exception {
        String poolName = task.getPoolName();
        if (!mapPool.containsKey(poolName)) {
            JdbcPool jdbcPool = new JdbcPool(poolName, 0, 1);
            mapPool.putIfAbsent(poolName, jdbcPool);
            jdbcPool.run();
        }
        JdbcPool jdbcPool = mapPool.get(poolName);
        jdbcPool.addResourceZeroPool();
        // -200 что бы коннект под нож статистики keepAlive не попал
        ConnectionEnvelope resource = jdbcPool.getResource(task.getMaxTimeExecute() - 200, threadEnvelope);

        if (resource == null) {
            throw new RuntimeException("Resource null");
        }
        TaskStatistic taskStatistic = getTaskStatistic(resource, task);
        List<Map<String, Object>> result = resource.exec(task);
        taskStatistic.finish();
        return result;
    }

}
