package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

// Уничтожающийся список элементов по времени. Если время объекта заканчивается и его не нейтрализовали
// объект передаётся в onExpired, иначе просто выбрасывается. Так как механизм зачистки вызывается планировщиком
// 1 раз в секунду, может быть лаг по времени, допустим: время жизни 1000мс, положили 00:00:00.050 ожидаем, что
// в 00:00:01.050 будет вызван onExpired, но нет, планировщик вызывался в 00:00:00.000 -> 00:00:01.000 -> 00:00:02.000
// то есть задержка onExpired может быть до секунды (это величина планировщика) больше или меньше будет изменяться в
// зависимости от того, когда вы в ExpirationList вставите данные. Для избежания одномоментного выполнения используется
// одноразовая обёртка (DisposableExpiredMsImmutableEnvelope). Время жизни объекта менять нельзя, так как
// не предусматривается реализацией ExpirationList

@Getter
public class ExpirationList<T>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        AddToList<
                ExpirationMsImmutableEnvelope<T>,
                DisposableExpirationMsImmutableEnvelope<T>
                >,
        ManagerElement, CascadeKey {


    private final String ns;

    public static Set<ExpirationList<?>> expirationListSet = Util.getConcurrentHashSet();

    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<T>>> bucket = new ConcurrentSkipListMap<>();

    private final ConcurrentSkipListMap<Long, AtomicInteger> bucketQueueSize = new ConcurrentSkipListMap<>();

    private final Consumer<DisposableExpirationMsImmutableEnvelope<T>> onExpired;

    // Сколько было просто удалено, так как объект был нейтрализован
    private final AtomicLong helperRemove = new AtomicLong(0);

    // Сколько было передано в обработчик OnExpired
    private final AtomicLong helperOnExpired = new AtomicLong(0);

    public ExpirationList(
            String ns,
            Consumer<DisposableExpirationMsImmutableEnvelope<T>> onExpired
    ) {
        this.ns = ns;
        this.onExpired = onExpired;
    }

    private void incQueueSize(Long key) {
        bucketQueueSize.computeIfAbsent(key, _ -> new AtomicInteger(0)).incrementAndGet();
    }

    // Уменьшить размер корзины, в том случае если она вообще существует
    private void deqQueueSize(Long key) {
        AtomicInteger atomicInteger = bucketQueueSize.get(key);
        if (atomicInteger != null) {
            atomicInteger.decrementAndGet();
        }
    }

    public List<Long> getBucketKey() {
        return bucket.keySet().stream().toList();
    }

    public void helper(AtomicBoolean threadRun, long curTimeMs) {
        UtilRisc.forEach(threadRun, bucket, (Long time, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<T>> queue) -> {
            if (time > curTimeMs) {
                return false;
            }
            UtilRisc.forEach(threadRun, queue, (DisposableExpirationMsImmutableEnvelope<T> envelope) -> {
                if (envelope.isNeutralized() || envelope.isStop()) {
                    queue.remove(envelope);
                    helperRemove.incrementAndGet();
                } else if (envelope.isExpired()) {
                    onExpired.accept(envelope);
                    queue.remove(envelope);
                    helperOnExpired.incrementAndGet();
                }
            });
            if (queue.isEmpty()) {
                bucket.remove(time);
                bucketQueueSize.remove(time);
                return true;
            }
            return false;
        });
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        AtomicInteger summaryCountItem = new AtomicInteger(0);
        AtomicInteger countBucket = new AtomicInteger(0);
        UtilRisc.forEach(
                threadRun,
                bucket,
                (Long time, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<T>> _) -> {
                    countBucket.incrementAndGet();
                    AtomicInteger s = bucketQueueSize.get(time);
                    if (s != null) {
                        summaryCountItem.addAndGet(s.get());
                    }
                }
        );
        result.add(new DataHeader()
                .setBody(getCascadeKey(ns))
                .put("ItemSize", summaryCountItem.get())
                .put("BucketSize", countBucket.get())
                .put("helperRemove", helperRemove.getAndSet(0))
                .put("helperOnExpired", helperOnExpired.getAndSet(0))
        );
        if (!bucket.isEmpty()) {
            markActive();
        }
        return result;
    }

    public DisposableExpirationMsImmutableEnvelope<T> add(T element, long timeOutMs) {
        return add(new ExpirationMsImmutableEnvelope<>(element, timeOutMs));
    }

    @Override
    public DisposableExpirationMsImmutableEnvelope<T> add(ExpirationMsImmutableEnvelope<T> obj) {
        return add(DisposableExpirationMsImmutableEnvelope.convert(obj));
    }

    public DisposableExpirationMsImmutableEnvelope<T> add(DisposableExpirationMsImmutableEnvelope<T> obj) {
        markActive();
        long timeMsExpiredFloor = Util.zeroLastNDigits(obj.getExpiredMs(), 3);
        bucket.computeIfAbsent(timeMsExpiredFloor, _ -> new ConcurrentLinkedQueue<>())
                .add(obj);
        incQueueSize(timeMsExpiredFloor);
        return obj;
    }

    public boolean remove(DisposableExpirationMsImmutableEnvelope<T> obj) {
        return remove(obj, true);
    }

    public boolean remove(DisposableExpirationMsImmutableEnvelope<T> obj, boolean doNeutralize) {
        // Удаление с нейтрализацией, если конечно doNeutralize = true
        boolean neutralize = doNeutralize && obj != null && obj.getValue() != null;
        // Если объект нейтрализован, будем перерасчитывать размер корзины
        if (neutralize) {
            deqQueueSize(Util.zeroLastNDigits(obj.getExpiredMs(), 3));
        }
        return neutralize;
    }

    @Override
    public void runOperation() {
        expirationListSet.add(this);
    }

    public boolean isEmpty() {
        return bucket.isEmpty() && bucketQueueSize.isEmpty();
    }

    @Override
    public void shutdownOperation() {
        // Я считаю это не гуманно при стопе чистить корзины, а вдруг его включат через секунду,
        // а мы уже удалил все данные. Я против такого.
        // Спустя много времени .... а на сколько гуманно делать остановку, того, что должно работать?
        bucket.clear();
        bucketQueueSize.clear();
        expirationListSet.remove(this);
    }

}
