package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.broker.BrokerQueueStatistic;
import ru.jamsys.statistic.BrokerStatistic;
import ru.jamsys.scheduler.SchedulerType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class Broker extends AbstractCoreComponent {

    private final Map<Class<?>, BrokerQueue<?>> mapQueue = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final ConfigurableApplicationContext applicationContext;
    private StatisticAggregator statisticAggregator; //Не можем сразу инициализировать из-за циклической зависимости

    public Broker(Scheduler scheduler, ConfigurableApplicationContext applicationContext) {
        this.scheduler = scheduler;
        this.applicationContext = applicationContext;
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_WRITE).add(this::flushStatistic);
    }

    @SuppressWarnings("unused")
    public <T> void add(Class<T> c, T o) throws Exception {
        BrokerQueue<T> brokerQueue = get(c);
        brokerQueue.add(o);
    }

    @SuppressWarnings("unused")
    public <T> BrokerQueue<T> get(Class<T> c) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!mapQueue.containsKey(c)) {
            mapQueue.putIfAbsent(c, new BrokerQueue<T>());
        }
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
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_WRITE).remove(this::flushStatistic);
        Object[] list = mapQueue.keySet().toArray();
        for (Object key : list) {
            BrokerQueue<?> brokerQueue = mapQueue.get(key);
            if (brokerQueue != null) {
                brokerQueue.shutdown();
            }
        }
        mapQueue.clear();
    }

    void init() {
        if (statisticAggregator == null) {
            statisticAggregator = applicationContext.getBean(StatisticAggregator.class);
        }
    }

    @Override
    public void flushStatistic() {
        init();
        BrokerStatistic<BrokerQueueStatistic> brokerStatistic = new BrokerStatistic<>();
        Util.riskModifier(mapQueue, new Class[0], (Class<?> cls, BrokerQueue<?> brokerQueue) -> brokerStatistic.getList().add(brokerQueue.flushStatistic()));
        statisticAggregator.add(brokerStatistic);
    }

}
