package ru.jamsys.cache;

import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class TimeCollectionExpired<T> implements StatisticsCollector, KeepAlive {

    final Map<Long, ConcurrentLinkedQueue<TimeEnvelope<T>>> map = new ConcurrentHashMap<>();

    final AtomicLong itemSize = new AtomicLong(0);

    final Consumer<T> onExpired;

    public TimeCollectionExpired(Consumer<T> onExpired) {
        this.onExpired = onExpired;
    }

    public TimeEnvelope<T> add(T obj, long curTime, int timeOutMs) {
        long timeMsExpired = curTime + timeOutMs;
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!map.containsKey(timeMsExpired)) { //Что бы лишний раз не создавать ConcurrentLinkedQueue при пробе putIfAbsent
            map.putIfAbsent(timeMsExpired, new ConcurrentLinkedQueue<>());
        }
        TimeEnvelope<T> timeEnvelope = new TimeEnvelope<>(obj);
        timeEnvelope.setKeepAliveOnInactivityMs(timeOutMs);
        itemSize.incrementAndGet();
        map.get(timeMsExpired).add(timeEnvelope);
        return timeEnvelope;
    }

    public TimeEnvelope<T> add(T obj, int timeOutMs) {
        return add(obj, System.currentTimeMillis(), timeOutMs);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("BucketSize", map.size())
                .addField("ItemSize", itemSize.get())
        );
        return result;
    }

    public void keepAlive(AtomicBoolean isRun, long curTimeMs) {
        Util.riskModifierMap(isRun, map, new Long[0], (Long timeMs, ConcurrentLinkedQueue<TimeEnvelope<T>> queue) -> {
            if (curTimeMs >= timeMs) {
                while (!queue.isEmpty()) {
                    TimeEnvelope<T> timeEnvelope = queue.poll();
                    if (!timeEnvelope.isStop()) {
                        onExpired.accept(timeEnvelope.getValue());
                    }
                    itemSize.decrementAndGet();
                }
                map.remove(timeMs);
            }
        });
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        keepAlive(isRun, System.currentTimeMillis());
    }
}
