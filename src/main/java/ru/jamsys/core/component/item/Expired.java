package ru.jamsys.core.component.item;

import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.util.ControlExpiredKeepAliveResult;
import ru.jamsys.core.util.ExpiredManagerEnvelope;
import ru.jamsys.core.util.Util;
import ru.jamsys.core.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Expired<V> implements KeepAlive, StatisticsFlush {
    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<ExpiredManagerEnvelope<V>>> bucket = new ConcurrentSkipListMap<>();

    public void add(V value, long expiredTimeMs, Consumer<V> onExpired) {
        long timeMsExpiredFloor = Util.zeroLastNDigits(expiredTimeMs, 3);
        bucket.computeIfAbsent(timeMsExpiredFloor, _ -> new ConcurrentLinkedQueue<>())
                .add(new ExpiredManagerEnvelope<>(value, onExpired));
    }

    public List<Long> getBucketKey() {
        return bucket.keySet().stream().toList();
    }

    public List<String> getBucketKeyFormat() {
        List<String> result = new ArrayList<>();
        getBucketKey().forEach(x -> result.add(Util.msToDataFormat(x)));
        return result;
    }

    public ControlExpiredKeepAliveResult keepAlive(AtomicBoolean isThreadRun, long curTimeMs) {
        ControlExpiredKeepAliveResult keepAliveResult = new ControlExpiredKeepAliveResult();
        UtilRisc.forEach(isThreadRun, bucket, (Long time, ConcurrentLinkedQueue<ExpiredManagerEnvelope<V>> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            UtilRisc.forEach(isThreadRun, queue, (ExpiredManagerEnvelope<V> expiredMsMutableEnvelope) -> {
                expiredMsMutableEnvelope.expire();
                queue.remove(expiredMsMutableEnvelope);
                keepAliveResult.getCountRemove().incrementAndGet();
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
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        UtilRisc.forEach(isThreadRun, bucket, (Long key, ConcurrentLinkedQueue<ExpiredManagerEnvelope<V>> value) -> {
            count.addAndGet(value.size());
        });
        result.add(new Statistic(parentTags, parentFields)
                .addField("size", count.get())
        );
        return result;
    }
}
