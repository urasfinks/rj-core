package ru.jamsys.component;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.broker.Queue;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.statistic.MapStatistic;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticFlush;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class Broker extends AbstractCoreComponent {

    private final Map<Class<?>, Queue<?>> mapQueue = new ConcurrentHashMap<>();
    private final Scheduler scheduler;

    public Broker(Scheduler scheduler, ConfigurableApplicationContext applicationContext) {
        this.scheduler = scheduler;
    }

    @SuppressWarnings("unused")
    public <T> void add(Class<T> cls, T o) throws Exception {
        Queue<T> queue = get(cls);
        queue.add(o);
    }

    @SuppressWarnings("unused")
    public <T> Queue<T> get(Class<T> cls) {
        //TODO: добавить маршрутизатор для класса
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!mapQueue.containsKey(cls)) {
            mapQueue.putIfAbsent(cls, new BrokerQueue<T>());
        }
        @SuppressWarnings("unchecked")
        Queue<T> queue = (Queue<T>) mapQueue.get(cls);
        return queue;
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
                mapQueue,
                new Class<?>[0],
                (Class<?> cls, Queue<?> queue) -> {
                    mapQueue.remove(cls);
                }
        );
    }

    @Override
    public void flushStatistic() {
        MapStatistic<Class<?>, Statistic> statistic = new MapStatistic<>();
        Util.riskModifierMap(
                mapQueue,
                new Class[0],
                (Class<?> cls, Queue<?> queue)
                        -> statistic.getMap().put(cls, ((StatisticFlush) queue).flushAndGetStatistic())
        );
        //TODO: что-то надо сделать) пока просто закоментил
        //statisticAggregator.add(getClass(), statistic);
    }

}
