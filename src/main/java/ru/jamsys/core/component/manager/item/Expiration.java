package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.flat.util.ExpirationKeepAliveResult;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Для задач, когда надо сформировать ошибки по timeOut
// Но надо помнить, что всегда есть лаг срабатывания onExpired, так как keepAlive вызывается по расписанию
// Уменьшить лаг можно путём более частого вызова keepAlive
// Мы не можем себе позволить постфактум менять timeout, так как в map заносится expiredTime из .getExpiredMs()
// Возвращается одноразовая обёртка, для синхронизации данных в многопоточном режиме
// Для избежания выполнения одномоментного выполнения используется одноразовая обёртка (DisposableExpiredMsImmutableEnvelope)

public class Expiration<V>
        extends ExpirationMsMutableImpl
        implements
        AddToList<
                ExpirationMsImmutableEnvelope<V>,
                DisposableExpirationMsImmutableEnvelope<V>
                >,
        KeepAlive,
        StatisticsFlush,
        ClassEquals,
        LifeCycleInterface,
        ExpirationMsMutable {

    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<V>>> bucket = new ConcurrentSkipListMap<>();

    private final ConcurrentSkipListMap<Long, AtomicInteger> bucketQueueSize = new ConcurrentSkipListMap<>();

    @Getter
    private final String index;

    private final Class<?> classItem;

    private final Consumer<DisposableExpirationMsImmutableEnvelope<?>> onExpired;

    public Expiration(String index, Class<?> classItem, Consumer<DisposableExpirationMsImmutableEnvelope<?>> onExpired) {
        this.index = index;
        this.classItem = classItem;
        this.onExpired = onExpired;
    }

    private void incQueueSize(Long key) {
        bucketQueueSize.computeIfAbsent(key, _ -> new AtomicInteger(0)).incrementAndGet();
    }

    private void deqQueueSize(Long key) {
        AtomicInteger atomicInteger = bucketQueueSize.get(key);
        if (atomicInteger != null) {
            atomicInteger.decrementAndGet();
        }
    }

    public List<Long> getBucketKey() {
        return bucket.keySet().stream().toList();
    }

    public ExpirationKeepAliveResult keepAlive(AtomicBoolean isThreadRun, long curTimeMs) {
        ExpirationKeepAliveResult keepAliveResult = new ExpirationKeepAliveResult();
        UtilRisc.forEach(isThreadRun, bucket, (Long time, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<V>> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            UtilRisc.forEach(isThreadRun, queue, (DisposableExpirationMsImmutableEnvelope<V> envelope) -> {
                if (envelope.isNeutralized() || envelope.isStop()) {
                    queue.remove(envelope);
                    keepAliveResult.getCountRemove().incrementAndGet();
                } else if (envelope.isExpired()) {
                    onExpired.accept(envelope);
                    queue.remove(envelope);
                    keepAliveResult.getCountRemove().incrementAndGet();
                }
            });
            if (queue.isEmpty()) {
                bucket.remove(time);
                bucketQueueSize.remove(time);
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
        AtomicInteger summaryCountItem = new AtomicInteger(0);
        AtomicInteger countBucket = new AtomicInteger(0);
        UtilRisc.forEach(
                isThreadRun,
                bucket,
                (Long time, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<V>> _) -> {
                    countBucket.incrementAndGet();
                    AtomicInteger s = bucketQueueSize.get(time);
                    if (s != null) {
                        summaryCountItem.addAndGet(s.get());
                    }
                }
        );
        result.add(new Statistic(parentTags, parentFields)
                .addField("ItemSize", summaryCountItem.get())
                .addField("BucketSize", countBucket.get())
        );
        if (!bucket.isEmpty()) {
            active();
        }
        return result;
    }

    @Override
    public DisposableExpirationMsImmutableEnvelope<V> add(ExpirationMsImmutableEnvelope<V> obj) {
        return add(DisposableExpirationMsImmutableEnvelope.convert(obj));
    }

    public DisposableExpirationMsImmutableEnvelope<V> add(DisposableExpirationMsImmutableEnvelope<V> obj) {
        active();
        long timeMsExpiredFloor = Util.zeroLastNDigits(obj.getExpiredMs(), 3);
        bucket.computeIfAbsent(timeMsExpiredFloor, _ -> new ConcurrentLinkedQueue<>())
                .add(obj);
        incQueueSize(timeMsExpiredFloor);
        return obj;
    }

    public void remove(DisposableExpirationMsImmutableEnvelope<V> obj) {
        remove(obj, true);
    }

    public void remove(DisposableExpirationMsImmutableEnvelope<V> obj, boolean neutralize) {
        if (neutralize && obj.getValue() == null) {
            return;
        }
        long timeMsExpiredFloor = Util.zeroLastNDigits(obj.getExpiredMs(), 3);
        deqQueueSize(timeMsExpiredFloor);
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return this.classItem.equals(classItem);
    }

    @Override
    public void run() {
        // Пока ничего не надо
    }

    @Override
    public void shutdown() {
        bucket.clear();
        bucketQueueSize.clear();
    }

}
