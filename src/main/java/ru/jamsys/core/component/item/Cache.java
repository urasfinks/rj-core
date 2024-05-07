package ru.jamsys.core.component.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;
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
        extends ExpiredMsMutableImpl
        implements StatisticsFlush, KeepAlive, Closable, AddToMap<K, ExpiredMsMutableEnvelope<TEO>> {

    @Getter
    final Map<K, ExpiredMsMutableEnvelope<TEO>> map = new ConcurrentHashMap<>();

    @Getter
    ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<K>> bucket = new ConcurrentSkipListMap<>();

    @Setter
    private Consumer<ExpiredMsMutableEnvelope<TEO>> onExpired;

    private final String index;

    public Cache(String index) {
        this.index = index;
    }

    @Override
    public void add(K key, ExpiredMsMutableEnvelope<TEO> value) {
        map.computeIfAbsent(key, s -> {
            long timeMsExpiredFloor = Util.zeroLastNDigits(value.getExpiredMs(), 3);
            bucket
                    .computeIfAbsent(timeMsExpiredFloor, s2 -> new ConcurrentLinkedQueue<>())
                    .add(key);
            return value;
        });
    }

    public ExpiredMsMutableEnvelope<TEO> add(K key, TEO value, long curTime, long timeoutMs) {
        ExpiredMsMutableEnvelope<TEO> expiredMsMutableEnvelope = new ExpiredMsMutableEnvelope<>(value);
        expiredMsMutableEnvelope.setKeepAliveOnInactivityMs(timeoutMs);
        expiredMsMutableEnvelope.setLastActivityMs(curTime);
        add(key, expiredMsMutableEnvelope);
        return expiredMsMutableEnvelope;
    }

    public ExpiredMsMutableEnvelope<TEO> add(K key, TEO value, long timeoutMs) {
        return add(key, value, System.currentTimeMillis(), timeoutMs);
    }

    public TEO get(K key) {
        ExpiredMsMutableEnvelope<TEO> expiredMsMutableEnvelope = map.get(key);
        if (expiredMsMutableEnvelope != null && !expiredMsMutableEnvelope.isExpired()) {
            expiredMsMutableEnvelope.stop();
            return expiredMsMutableEnvelope.getValue();
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
                ExpiredMsMutableEnvelope<TEO> expiredMsMutableEnvelope = map.get(key);
                if (expiredMsMutableEnvelope != null) {
                    if (expiredMsMutableEnvelope.isExpired()) {
                        if (onExpired != null) {
                            onExpired.accept(expiredMsMutableEnvelope);
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
