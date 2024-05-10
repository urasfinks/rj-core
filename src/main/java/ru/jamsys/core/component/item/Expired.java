package ru.jamsys.core.component.item;

import lombok.Setter;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutableEnvelope;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutable;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;
import ru.jamsys.core.util.ControlExpiredKeepAliveResult;
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
public class Expired<V>
        extends ExpiredMsMutableImpl
        implements
        AddToList<
                ExpiredMsImmutableEnvelope<V>,
                ExpiredMsImmutableEnvelope<V>
                >,
        Closable,
        KeepAlive,
        StatisticsFlush,
        ExpiredMsMutable {

    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<ExpiredMsImmutableEnvelope<V>>> bucket = new ConcurrentSkipListMap<>();

    private final String index;

    @Setter
    private Consumer<ExpiredMsImmutableEnvelope<V>> onExpired;

    public Expired(String index) {
        this.index = index;
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
        UtilRisc.forEach(isThreadRun, bucket, (Long time, ConcurrentLinkedQueue<ExpiredMsImmutableEnvelope<V>> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            UtilRisc.forEach(isThreadRun, queue, (ExpiredMsImmutableEnvelope<V> expiredMsMutableEnvelope) -> {
                if (expiredMsMutableEnvelope.isExpired()) {
                    onExpired.accept(expiredMsMutableEnvelope);
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

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        UtilRisc.forEach(isThreadRun, bucket, (Long key, ConcurrentLinkedQueue<ExpiredMsImmutableEnvelope<V>> value) -> count.addAndGet(value.size()));
        result.add(new Statistic(parentTags, parentFields)
                .addField("ItemSize", count.get())
                .addField("BucketSize", bucket.size())
        );
        return result;
    }

    @Override
    public void close() {
        bucket.clear();
    }

    @Override
    public ExpiredMsImmutableEnvelope<V> add(ExpiredMsImmutableEnvelope<V> obj) throws Exception {
        long timeMsExpiredFloor = Util.zeroLastNDigits(obj.getExpiredMs(), 3);
        bucket.computeIfAbsent(timeMsExpiredFloor, _ -> new ConcurrentLinkedQueue<>())
                .add(obj);
        return obj;
    }

}
