package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.statistic.MapStatistic;
import ru.jamsys.statistic.Statistic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class Broker extends AbstractCoreComponent {

    private final Map<Class<?>, BrokerQueue<?>> mapBrokerQueue = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final ConfigurableApplicationContext applicationContext;
    private StatisticAggregator statisticAggregator; //Не можем сразу инициализировать из-за циклической зависимости

    public Broker(Scheduler scheduler, ConfigurableApplicationContext applicationContext) {
        this.scheduler = scheduler;
        this.applicationContext = applicationContext;
    }

    public void run() {
        statisticAggregator = applicationContext.getBean(StatisticAggregator.class);
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_WRITE).add(this::flushStatistic);
    }

    @SuppressWarnings("unused")
    public <T> void add(Class<T> cls, T o) throws Exception {
        BrokerQueue<T> brokerQueue = get(cls);
        brokerQueue.add(o);
    }

    @SuppressWarnings("unused")
    public <T> BrokerQueue<T> get(Class<T> cls) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!mapBrokerQueue.containsKey(cls)) {
            mapBrokerQueue.putIfAbsent(cls, new BrokerQueue<T>());
        }
        @SuppressWarnings("unchecked")
        BrokerQueue<T> brokerQueue = (BrokerQueue<T>) mapBrokerQueue.get(cls);
        return brokerQueue;
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
        Util.riskModifierMap(
                mapBrokerQueue,
                new Class<?>[0],
                (Class<?> cls, BrokerQueue<?> brokerQueue) -> {
                    brokerQueue.shutdown();
                    mapBrokerQueue.remove(cls);
                }
        );
    }

    @Override
    public void flushStatistic() {
        MapStatistic<Class<?>, Statistic> statistic = new MapStatistic<>();
        Util.riskModifierMap(
                mapBrokerQueue,
                new Class[0],
                (Class<?> cls, BrokerQueue<?> brokerQueue)
                        -> statistic.getMap().put(cls, brokerQueue.flushAndGetStatistic())
        );
        statisticAggregator.add(getClass(), statistic);
    }

}
