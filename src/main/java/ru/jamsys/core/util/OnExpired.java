package ru.jamsys.core.util;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class OnExpired<V> implements KeepAlive {

    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<ExpiredMsImmutableEnvelope<V>>> bucket = new ConcurrentSkipListMap<>();

    private final Consumer<ExpiredMsImmutableEnvelope<V>> onExpired;

    public OnExpired(Consumer<ExpiredMsImmutableEnvelope<V>> onExpired) {
        this.onExpired = onExpired;
    }

    public void add(ExpiredMsImmutableEnvelope<V> value) {
        long timeMsExpiredFloor = Util.zeroLastNDigits(value.getExpiredMs(), 3);
        bucket.computeIfAbsent(timeMsExpiredFloor, _ -> new ConcurrentLinkedQueue<>()).add(value);
    }

    public List<Long> getBucketKey() {
        return bucket.keySet().stream().toList();
    }

    public List<String> getBucketKeyFormat() {
        List<String> result = new ArrayList<>();
        getBucketKey().forEach(x -> result.add(Util.msToDataFormat(x)));
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

    public KeepAliveResult keepAlive(AtomicBoolean isThreadRun, long curTimeMs) {
        KeepAliveResult keepAliveResult = new KeepAliveResult();
        Util.riskModifierMapBreak(isThreadRun, bucket, Util.getEmptyType(), (Long time, ConcurrentLinkedQueue<ExpiredMsImmutableEnvelope<V>> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            Util.riskModifierCollection(isThreadRun, queue,Util.getEmptyType(), (ExpiredMsImmutableEnvelope<V> expiredMsMutableEnvelope) -> {
                if (expiredMsMutableEnvelope.isExpired()) {
                    if (onExpired != null) {
                        onExpired.accept(expiredMsMutableEnvelope);
                    }
                    queue.remove(expiredMsMutableEnvelope);
                    keepAliveResult.getCountRemove().incrementAndGet();
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

}
