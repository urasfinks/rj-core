package ru.jamsys.core.util;

import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ControlExpired<V> implements KeepAlive {

    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<V>> bucket = new ConcurrentSkipListMap<>();

    private final Consumer<V> onExpired;

    public ControlExpired(Consumer<V> onExpired) {
        this.onExpired = onExpired;
    }

    public void add(ExpiredMsImmutableEnvelope<V> value) {
        long timeMsExpiredFloor = Util.zeroLastNDigits(value.getExpiredMs(), 3);
        bucket.computeIfAbsent(timeMsExpiredFloor, _ -> new ConcurrentLinkedQueue<>()).add(value.getValue());
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
        Util.riskModifierMapBreak(isThreadRun, bucket, Util.getEmptyType(), (Long time, ConcurrentLinkedQueue<V> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            Util.riskModifierCollection(isThreadRun, queue, Util.getEmptyType(), (V expiredMsMutableEnvelope) -> {
                onExpired.accept(expiredMsMutableEnvelope);
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

}
