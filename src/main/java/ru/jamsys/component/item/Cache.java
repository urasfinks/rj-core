package ru.jamsys.component.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.component.base.AddableMapElement;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TimeControllerImpl;
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

//Мы не можем себе позволить постфактум менять timeout, так как в map заносится expiredTime из .getExpiredMs()

public class Cache<
        K,
        TEO
        > extends TimeControllerImpl
        implements StatisticsCollector, KeepAlive, Closable, AddableMapElement<K, TimeEnvelope<TEO>, TimeEnvelope<TEO>> {

    @Getter
    final Map<K, TimeEnvelope<TEO>> map = new ConcurrentHashMap<>();

    @Getter
    ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<K>> bucket = new ConcurrentSkipListMap<>();

    @Setter
    private Consumer<TimeEnvelope<TEO>> onExpired;

    @Override
    public TimeEnvelope<TEO> add(K key, TimeEnvelope<TEO> value) throws Exception {
        if (!map.containsKey(key)) {
            map.put(key, value);
            long timeMsExpiredFloor = Util.zeroLastNDigits(value.getExpiredMs(), 3);
            if (!bucket.containsKey(timeMsExpiredFloor)) {
                bucket.putIfAbsent(timeMsExpiredFloor, new ConcurrentLinkedQueue<>());
            }
            bucket.get(timeMsExpiredFloor).add(key);
        }
        return value;
    }

    public TimeEnvelope<TEO> add(K key, TEO value, long curTime, long timeoutMs) throws Exception {
        TimeEnvelope<TEO> timeEnvelope = new TimeEnvelope<>(value);
        timeEnvelope.setKeepAliveOnInactivityMs(timeoutMs);
        timeEnvelope.setLastActivity(curTime);
        add(key, timeEnvelope);
        return timeEnvelope;
    }

    public TimeEnvelope<TEO> add(K key, TEO value, long timeoutMs) throws Exception {
        return add(key, value, System.currentTimeMillis(), timeoutMs);
    }

    public TEO get(K key) {
        TimeEnvelope<TEO> timeEnvelope = map.get(key);
        if (timeEnvelope != null && !timeEnvelope.isExpired()) {
            timeEnvelope.stop();
            return timeEnvelope.getValue();
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
                TimeEnvelope<TEO> timeEnvelope = map.get(key);
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

    @Override
    public void close() {

    }

}
