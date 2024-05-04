package ru.jamsys.core.component.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.addable.AddableMapItem;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.TimeControllerMsImpl;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Для задач когда надо прихранить какие-либо данные на время по ключу
// Для задач, когда надо сформировать ошибки, если какие-либо задачи не исполнились
// Но надо помнить, что всегда есть лаг срабатывания onExpired, так как keepAlive вызывается по расписанию
// Уменьшить лаг можно путём более частого вызова keepAlive

//Мы не можем себе позволить постфактум менять timeout, так как в map заносится expiredTime из .getExpiredMs()

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Cache<K, TEO>
        extends TimeControllerMsImpl
        implements StatisticsFlush, KeepAlive, Closable, AddableMapItem<K, TimeEnvelopeMs<TEO>> {

    @Getter
    final Map<K, TimeEnvelopeMs<TEO>> map = new ConcurrentHashMap<>();

    @Getter
    ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<K>> bucket = new ConcurrentSkipListMap<>();

    @Setter
    private Consumer<TimeEnvelopeMs<TEO>> onExpired;

    @Override
    public void add(K key, TimeEnvelopeMs<TEO> value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
            long timeMsExpiredFloor = Util.zeroLastNDigits(value.getExpiredMs(), 3);
            if (!bucket.containsKey(timeMsExpiredFloor)) {
                bucket.putIfAbsent(timeMsExpiredFloor, new ConcurrentLinkedQueue<>());
            }
            bucket.get(timeMsExpiredFloor).add(key);
        }
    }

    public TimeEnvelopeMs<TEO> add(K key, TEO value, long curTime, long timeoutMs) {
        TimeEnvelopeMs<TEO> timeEnvelopeMs = new TimeEnvelopeMs<>(value);
        timeEnvelopeMs.setKeepAliveOnInactivityMs(timeoutMs);
        timeEnvelopeMs.setLastActivityMs(curTime);
        add(key, timeEnvelopeMs);
        return timeEnvelopeMs;
    }

    public TimeEnvelopeMs<TEO> add(K key, TEO value, long timeoutMs) {
        return add(key, value, System.currentTimeMillis(), timeoutMs);
    }

    public TEO get(K key) {
        TimeEnvelopeMs<TEO> timeEnvelopeMs = map.get(key);
        if (timeEnvelopeMs != null && !timeEnvelopeMs.isExpired()) {
            timeEnvelopeMs.stop();
            return timeEnvelopeMs.getValue();
        }
        return null;
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
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
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
    public KeepAliveResult keepAlive(AtomicBoolean isThreadRun, long curTimeMs) {
        KeepAliveResult keepAliveResult = new KeepAliveResult();
        Util.riskModifierMapBreak(isThreadRun, bucket, getEmptyType(), (Long time, ConcurrentLinkedQueue<K> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            Util.riskModifierCollection(isThreadRun, queue, getEmptyType(), (K key) -> {
                TimeEnvelopeMs<TEO> timeEnvelopeMs = map.get(key);
                if (timeEnvelopeMs != null) {
                    if (timeEnvelopeMs.isExpired()) {
                        if (onExpired != null) {
                            onExpired.accept(timeEnvelopeMs);
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
    public void keepAlive(AtomicBoolean isThreadRun) {
        keepAlive(isThreadRun, System.currentTimeMillis());
    }

    @Override
    public void close() {

    }

}
