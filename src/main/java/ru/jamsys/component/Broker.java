package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.broker.BrokerStatistic;
import ru.jamsys.scheduler.SchedulerGlobal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class Broker extends AbstractCoreComponent {

    private final Map<Class<?>, BrokerQueue<?>> mapQueue = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final ConfigurableApplicationContext applicationContext;
    private StatisticAggregator statisticAggregator;

    public Broker(Scheduler scheduler, ConfigurableApplicationContext applicationContext) {
        this.scheduler = scheduler;
        this.applicationContext = applicationContext;
        scheduler.add(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_WRITE, this::flushStatistic);
    }

    @SuppressWarnings("unused")
    public <T> void add(Class<T> c, T o) throws Exception {
        BrokerQueue<T> brokerQueue = get(c);
        brokerQueue.add(o);
    }

    @SuppressWarnings("unused")
    public <T> BrokerQueue<T> get(Class<T> c) {
        mapQueue.putIfAbsent(c, new BrokerQueue<T>());
        return (BrokerQueue<T>) mapQueue.get(c);
    }

    @SuppressWarnings("unused")
    public <T> T pollLast(Class<T> c) {
        return get(c).pollLast();
    }

    @SuppressWarnings("unused")
    public <T> T pollFirst(Class<T> c) {
        return get(c).pollFirst();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.remove(SchedulerGlobal.SCHEDULER_GLOBAL_STATISTIC_WRITE, this::flushStatistic);
        Object[] list = mapQueue.keySet().toArray();
        for (Object key : list) {
            BrokerQueue<?> brokerQueue = mapQueue.get(key);
            if (brokerQueue != null) {
                brokerQueue.shutdown();
            }
        }
        mapQueue.clear();
    }

    @Override
    public void flushStatistic() {
        Class<?>[] objects = mapQueue.keySet().toArray(new Class[0]);
        if (objects.length > 0) {
            BrokerStatistic brokerStatistic = new BrokerStatistic();
            for (Class<?> key : objects) {
                BrokerQueue<?> brokerQueue = mapQueue.get(key);
                brokerStatistic.getList().add(brokerQueue.flushStatistic());
            }
            if (statisticAggregator == null) {
                statisticAggregator = applicationContext.getBean(StatisticAggregator.class);
            }
            statisticAggregator.add(brokerStatistic);
        }
    }

}
