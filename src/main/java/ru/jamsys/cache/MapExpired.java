package ru.jamsys.cache;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Для задач когда надо прихранить какие-либо данные на время по ключу
// Для задач, когда надо сформировать ошибки, если какие-либо задачи не исполнились
// Но надо помнить, что всегда есть лаг срабатывания onExpired, так как keepAlive вызывается по расписанию
// Уменьшить лаг можно путём более частого вызова keepAlive

public class MapExpired<K, V> implements StatisticsCollector, KeepAlive {

    final Map<K, TimeEnvelope<V>> map = new ConcurrentHashMap<>();

    @Getter
    ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<K>> sort = new ConcurrentSkipListMap<>();

    @Setter
    private Consumer<V> onExpired;

    public boolean add(K key, V value, long curTime, long timeoutMs) {
        if (!map.containsKey(key)) {

            TimeEnvelope<V> timeEnvelope = new TimeEnvelope<>(value);
            timeEnvelope.setKeepAliveOnInactivityMs(timeoutMs);
            timeEnvelope.setLastActivity(curTime);

            map.put(key, timeEnvelope);
            long timeMsExpired = Util.zeroLastNDigits(curTime + timeoutMs, 3);
            if (!sort.containsKey(timeMsExpired)) {
                sort.putIfAbsent(timeMsExpired, new ConcurrentLinkedQueue<>());
            }
            sort.get(timeMsExpired).add(key);
            return true;
        }
        return false;
    }

    public boolean add(K key, V value, long timeoutMs) {
        return add(key, value, System.currentTimeMillis(), timeoutMs);
    }

    public V get(K key) {
        TimeEnvelope<V> timeEnvelope = map.get(key);
        if (timeEnvelope != null && !timeEnvelope.isExpired()) {
            timeEnvelope.stop();
            return timeEnvelope.getValue();
        }
        return null;
    }

    public Map<K, TimeEnvelope<V>> get() {
        return map;
    }

    @SafeVarargs
    static <K> K[] getEmptyType(K... array) {
        return Arrays.copyOf(array, 0);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("MapSize", map.size())
                .addField("BucketSize", map.size())
        );
        return result;
    }

    public void keepAlive(AtomicBoolean isRun, long curMs) {
        Util.riskModifierMapBreak(isRun, sort, getEmptyType(), (Long time, ConcurrentLinkedQueue<K> queue) -> {
            if (time > curMs) {
                return false;
            }
            Util.riskModifierCollection(isRun, queue, getEmptyType(), (K key) -> {
                TimeEnvelope<V> timeEnvelope = map.get(key);
                if (timeEnvelope != null) {
                    if (timeEnvelope.isExpired()) {
                        if (onExpired != null) {
                            onExpired.accept(timeEnvelope.getValue());
                        }
                        queue.remove(key);
                        map.remove(key);
                    }
                }
            });
            if (queue.isEmpty()) {
                sort.remove(time);
                return true;
            }
            return false;
        });
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        keepAlive(isRun, System.currentTimeMillis());
    }

}
