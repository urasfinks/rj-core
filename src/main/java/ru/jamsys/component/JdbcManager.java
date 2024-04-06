package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.jdbc.ConnectionEnvelope;
import ru.jamsys.pool.AutoBalancerPools;
import ru.jamsys.pool.JdbcPool;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.JdbcRequest;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Component
@Lazy
public class JdbcManager extends AutoBalancerPools<JdbcPool, ConnectionEnvelope>
        implements KeepAliveComponent, StatisticsCollectorComponent {

    public JdbcManager(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public List<Map<String, Object>> execTask(JdbcRequest task, ThreadEnvelope threadEnvelope) throws Exception {
        String poolName = task.getPoolName();
        JdbcPool jdbcPool = getItem(poolName);
        jdbcPool.addPoolItemIfEmpty();
        // -200 что бы коннект под нож статистики keepAlive не попал
        ConnectionEnvelope connectionEnvelope = jdbcPool.getPoolItem(task.getExpiryRemainingMs() - 200, threadEnvelope);
        if (connectionEnvelope == null) {
            throw new RuntimeException("connectionEnvelope is null");
        }
        TaskStatistic taskStatistic = getTaskStatistic(connectionEnvelope, task);
        List<Map<String, Object>> result = connectionEnvelope.exec(task);
        taskStatistic.stop();
        return result;
    }

    @Override
    public JdbcPool build(String key) {
        JdbcPool jdbcPool = new JdbcPool(key, 0, 1);
        jdbcPool.run();
        return jdbcPool;
    }

}
