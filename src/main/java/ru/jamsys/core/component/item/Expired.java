package ru.jamsys.core.component.item;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.immutable.DisposableExpiredMsImmutableEnvelope;
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

// Для задач, когда надо сформировать ошибки по timeOut
// Но надо помнить, что всегда есть лаг срабатывания onExpired, так как keepAlive вызывается по расписанию
// Уменьшить лаг можно путём более частого вызова keepAlive
// Мы не можем себе позволить постфактум менять timeout, так как в map заносится expiredTime из .getExpiredMs()
// Возвращается одноразовая обёртка, для синхронизации данных в многопоточном режиме
// Если не хотите выполнить код onExpired и какую либо бизнеслогику в одномоментном протухахании - используйте
// DisposableExpiredMsImmutableEnvelope в своей логике, дабы избежать конфликтов

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Expired<V>
        extends ExpiredMsMutableImpl
        implements
        AddToList<
                ExpiredMsImmutableEnvelope<V>,
                DisposableExpiredMsImmutableEnvelope<V>
                >,
        Closable,
        KeepAlive,
        StatisticsFlush,
        ExpiredMsMutable {

    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<DisposableExpiredMsImmutableEnvelope<V>>> bucket = new ConcurrentSkipListMap<>();

    @Getter
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
        UtilRisc.forEach(isThreadRun, bucket, (Long time, ConcurrentLinkedQueue<DisposableExpiredMsImmutableEnvelope<V>> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            keepAliveResult.getReadBucket().add(time);
            UtilRisc.forEach(isThreadRun, queue, (DisposableExpiredMsImmutableEnvelope<V> disposableExpiredMsImmutableEnvelope) -> {
                if (disposableExpiredMsImmutableEnvelope.isExpired()) {
                    V v = disposableExpiredMsImmutableEnvelope.concurrentUse();
                    if (v != null) {
                        onExpired.accept(disposableExpiredMsImmutableEnvelope);
                    }
                    queue.remove(disposableExpiredMsImmutableEnvelope);
                    keepAliveResult.getCountRemove().incrementAndGet();
                } else if (disposableExpiredMsImmutableEnvelope.isStop()) {
                    queue.remove(disposableExpiredMsImmutableEnvelope);
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
        UtilRisc.forEach(
                isThreadRun,
                bucket,
                (Long key, ConcurrentLinkedQueue<DisposableExpiredMsImmutableEnvelope<V>> queue)
                        -> count.addAndGet(queue.size())
        );
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
    public DisposableExpiredMsImmutableEnvelope<V> add(ExpiredMsImmutableEnvelope<V> obj) {
        DisposableExpiredMsImmutableEnvelope<V> convert = DisposableExpiredMsImmutableEnvelope.convert(obj);
        long timeMsExpiredFloor = Util.zeroLastNDigits(convert.getExpiredMs(), 3);
        bucket.computeIfAbsent(timeMsExpiredFloor, _ -> new ConcurrentLinkedQueue<>())
                .add(convert);
        return convert;
    }

}
