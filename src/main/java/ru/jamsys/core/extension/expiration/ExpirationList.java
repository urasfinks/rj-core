package ru.jamsys.core.extension.expiration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

// Уничтожающийся список элементов по времени. Если время объекта заканчивается и его не нейтрализовали
// объект передаётся в onExpired, иначе просто выбрасывается. Так как механизм зачистки вызывается планировщиком
// 1 раз в секунду, может быть лаг по времени, допустим: время жизни 1000мс, положили 00:00:00.050 ожидаем, что
// в 00:00:01.050 будет вызван onExpired, но нет, планировщик вызывался в 00:00:00.000 -> 00:00:01.000 -> 00:00:02.000,
// то есть задержка onExpired может быть до секунды (это величина планировщика) Для избежания одномоментного
// выполнения используется одноразовая обёртка (DisposableExpiredMsImmutableEnvelope). Время жизни объекта менять
// нельзя, так как не предусматривается реализацией ExpirationList

@Getter
public class ExpirationList<T>
        extends AbstractManagerElement
        implements
        AddToList<
                ExpirationMsImmutableEnvelope<T>,
                DisposableExpirationMsImmutableEnvelope<T>
                > {

    private final String ns;

    private final String key;

    @JsonIgnore
    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<T>>> bucket = new ConcurrentSkipListMap<>();

    private final ConcurrentSkipListMap<Long, AtomicInteger> bucketQueueSize = new ConcurrentSkipListMap<>();

    private Consumer<T> onExpired;

    // Сколько было просто удалено, так как объект был нейтрализован
    private final AtomicLong helperRemove = new AtomicLong(0);

    // Сколько было передано в OnExpired
    private final AtomicLong helperOnExpired = new AtomicLong(0);

    public ExpirationList(String ns, String key) {
        this.ns = ns;
        this.key = key;
    }

    public void setupOnExpired(Consumer<T> onExpired){
        this.onExpired = onExpired;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                ;
    }

    private void incQueueSize(Long key) {
        bucketQueueSize.computeIfAbsent(key, _ -> new AtomicInteger(0)).incrementAndGet();
    }

    // Уменьшить размер корзины, в том случае если она вообще существует
    private void deqQueueSize(Long key) {
        AtomicInteger atomicInteger = bucketQueueSize.get(key);
        if (atomicInteger != null && atomicInteger.decrementAndGet() == 0) {
            // Нам надо удалять пустые ключи, что бы правильно работало ExpirationList.isEmpty() в отличие от
            // bucket там элементы нейтрализуются, а не удаляются, так что это на данный момент единственный способ
            // проверить, что ExpirationList.isEmpty() = true, проверкой bucketQueueSize.isEmpty()
            bucketQueueSize.remove(key);
        }
    }

    public List<Long> getBucketKey() {
        return bucket.keySet().stream().toList();
    }

    public void helper(AtomicBoolean threadRun, long curTimeMs) {
        // В bucket лежат отсортированные по времени удаления очереди данных
        for (Map.Entry<Long, ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<T>>> entry : bucket.entrySet()) {
            if (!threadRun.get()) {
                break;
            }
            Long time = entry.getKey();
            if (time > curTimeMs) {
                break;
            }
            ConcurrentLinkedQueue<DisposableExpirationMsImmutableEnvelope<T>> queue = entry.getValue();
            if (queue != null) {
                while (!queue.isEmpty()) {
                    if (!threadRun.get()) {
                        break;
                    }
                    DisposableExpirationMsImmutableEnvelope<T> envelope = queue.poll();
                    if (envelope == null) {
                        UtilLog.printError(this);
                        continue;
                    }
                    if (envelope.isNeutralized() || envelope.isStopped()) {
                        helperRemove.incrementAndGet();
                    } else if (envelope.isExpired()) {
                        T value = envelope.getValue();
                        if (value != null) {
                            if (value instanceof ExpirationDrop expirationDrop) {
                                try {
                                    expirationDrop.onExpirationDrop();
                                } catch (Throwable th) {
                                    App.error(th);
                                }
                            }
                            if (onExpired != null) {
                                onExpired.accept(value);
                            }
                            helperOnExpired.incrementAndGet();
                        }
                    }
                }
            }
            bucket.remove(time);
            bucketQueueSize.remove(time);
        }
    }

    @Override
    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<StatisticDataHeader> result = new ArrayList<>();
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
        result.add(new StatisticDataHeader(getClass(), ns)
                .addHeader("item", summaryCountItem.get())
                .addHeader("bucket", countBucket.get())
                .addHeader("remove", helperRemove.getAndSet(0))
                .addHeader("expired", helperOnExpired.getAndSet(0))
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
        // В следующую секунду будем удалять, что бы очереди на удаление удалять полностью
        long timeMsExpired = Util.resetLastNDigits(obj.getExpirationTimeMs(), 3) + 1_000L;
        bucket.computeIfAbsent(timeMsExpired, _ -> new ConcurrentLinkedQueue<>())
                .add(obj);
        incQueueSize(timeMsExpired);
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
            deqQueueSize(Util.resetLastNDigits(obj.getExpirationTimeMs(), 3) + 1_000L);
        }
        return neutralize;
    }

    public int size() {
        return bucketQueueSize.values()
                .stream()
                .mapToInt(AtomicInteger::get)
                .sum();
    }

    @Override
    public void runOperation() { }

    public boolean isEmpty() {
        // bucket может быть не пустой так как remove только нейтрализует объекта, без реального удаления,
        // а вот bucketQueueSize в deqQueueSize удаляет ключи, если размер становится равен 0
        return bucketQueueSize.isEmpty();
    }

    @Override
    public void shutdownOperation() {
        // Я считаю это не гуманно при стопе чистить корзины, а вдруг его включат через секунду,
        // а мы уже удалил все данные. Я против такого.
        // Спустя много времени .... а на сколько гуманно делать остановку, того, что должно работать?
        bucket.clear();
        bucketQueueSize.clear();
    }

    public void unitTestReset(){
        bucket.clear();
        bucketQueueSize.clear();
        helperRemove.set(0);
        helperOnExpired.set(0);
    }

}
