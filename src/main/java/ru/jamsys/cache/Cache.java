package ru.jamsys.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Для задач когда надо прихранить какие-либо данные на время по ключу
// Для задач, когда надо сформировать ошибки, если какие-либо задачи не исполнились
// Но надо помнить, что всегда есть лаг срабатывания onExpired, так как keepAlive вызывается по расписанию
// Уменьшить лаг можно путём более частого вызова keepAlive

public class Cache<K, V> implements StatisticsCollector, KeepAlive {

    final Map<K, TimeEnvelope<V>> map = new ConcurrentHashMap<>();

    @Getter
    ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<K>> bucket = new ConcurrentSkipListMap<>();

    @Setter
    private Consumer<TimeEnvelope<V>> onExpired;

    public boolean add(K key, V value, long curTime, long timeoutMs) {
        if (!map.containsKey(key)) {
            TimeEnvelope<V> timeEnvelope = new TimeEnvelope<>(value);
            timeEnvelope.setKeepAliveOnInactivityMs(timeoutMs);
            timeEnvelope.setLastActivity(curTime);

            map.put(key, timeEnvelope);
            long timeMsExpired = Util.zeroLastNDigits(curTime + timeoutMs, 3);
            if (!bucket.containsKey(timeMsExpired)) {
                bucket.putIfAbsent(timeMsExpired, new ConcurrentLinkedQueue<>());
            }
            bucket.get(timeMsExpired).add(key);
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

    public List<Long> getBucketKey() {
        return bucket.keySet().stream().toList();
    }

    @SuppressWarnings("unused")
    public List<String> getBucketKeyFormat() {
        List<String> result = new ArrayList<>();
        getBucketKey().forEach(x -> result.add(Util.msToDataFormat(x)));
        return result;
    }

    @SafeVarargs
    static <K> K[] getEmptyType(K... array) {
        return Arrays.copyOf(array, 0);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("MapSize", map.size())
                .addField("BucketSize", bucket.size())
        );
        return result;
    }

    @Getter
    @ToString
    public static class KeepAliveResult {
        List<Long> readBucket = new ArrayList<>();
        AtomicInteger countRemove = new AtomicInteger(0);

        public List<String> getReadBucketFormat() {
            List<String> result = new ArrayList<>();
            readBucket.forEach(x -> result.add(Util.msToDataFormat(x)));
            return result;
        }
    }

    //Для тестирования будем возвращать пачку на которой
    public KeepAliveResult keepAlive(ThreadEnvelope threadEnvelope, long curTimeMs) {
        KeepAliveResult keepAliveResult = new KeepAliveResult();
        Util.riskModifierMapBreak(threadEnvelope.getIsWhile(), bucket, getEmptyType(), (Long time, ConcurrentLinkedQueue<K> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            Util.riskModifierCollection(threadEnvelope.getIsWhile(), queue, getEmptyType(), (K key) -> {
                TimeEnvelope<V> timeEnvelope = map.get(key);
                if (timeEnvelope != null) {
                    if (timeEnvelope.isExpired()) {
                        if (onExpired != null) {
                            onExpired.accept(timeEnvelope);
                        }
                        queue.remove(key);
                        map.remove(key);
                        keepAliveResult.getCountRemove().incrementAndGet();
                    }
                }
            });
            if (queue.isEmpty()) {
                bucket.remove(time);
                return true;
            }
            return false;
        });
        return keepAliveResult;
    }

    @Override
    public void keepAlive(ThreadEnvelope threadEnvelope) {
        keepAlive(threadEnvelope, System.currentTimeMillis());
    }

}
