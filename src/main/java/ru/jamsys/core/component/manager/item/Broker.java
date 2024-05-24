package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.RateLimitManager;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Раньше на вставку был просто объект TEO и внутри происходила обёртка TimeEnvelope<TEO>
// Но потом пришла реализация Cache где нельзя было такое сделать
// и на вход надо было уже подавать объект TimeEnvelope<TEO>
//     Причина: сначала необходимо выставлять время, на которое будем кешировать, а только потом делать вставку
//              Индекс рассчитывается из времени протухания
//              После установки время протухания изменить его нельзя, так как индекс уже ключом Map
// Я захотел, что бы везде было одинаково. Только лишь поэтому TEO -> TimeEnvelope<TEO>
// 11.05.2024 Cache перехал в Session, и вся история с однотипным протоколом распалась

//Время срабатывания onExpired = 3 секунды

//TODO: сделать TEO наследуемым от getIndex (RqUID)
@Component
@Scope("prototype")
@Getter
@Setter
//@IgnoreClassFinder // не знаю почему он был исключён
public class Broker<TEO>
        extends ExpirationMsMutableImpl
        implements
        ClassName,
        StatisticsFlush,
        Closable,
        KeepAlive,
        CheckClassItem,
        AddToList<
                ExpirationMsImmutableEnvelope<TEO>,
                DisposableExpirationMsImmutableEnvelope<TEO> // Должны вернуть, что бы из вне можно было сделать remove
                > {

    private final ConcurrentLinkedDeque<DisposableExpirationMsImmutableEnvelope<TEO>> queue = new ConcurrentLinkedDeque<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<ExpirationMsImmutableEnvelope<TEO>> tail = new ConcurrentLinkedDeque<>();

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    private boolean cyclical = true;

    final RateLimit rateLimit;

    final RateLimitItem rliQueueSize;

    final RateLimitItem rliTailSize;

    final RateLimitItem rliTps;

    final String index;

    private Consumer<TEO> onDrop = null;

    private Class<TEO> classItem;

    public Broker(String index, ApplicationContext applicationContext, Class<TEO> classItem) {
        this.index = index;
        this.classItem = classItem;

        rateLimit = applicationContext.getBean(RateLimitManager.class).get(getClassName(index, applicationContext))
                .init(RateLimitName.BROKER_SIZE.getName(), RateLimitItemInstance.MAX)
                .init(RateLimitName.BROKER_TAIL_SIZE.getName(), RateLimitItemInstance.MAX)
                .init(RateLimitName.BROKER_TPS.getName(), RateLimitItemInstance.TPS);

        rliQueueSize = rateLimit.get(RateLimitName.BROKER_SIZE.getName());
        rliQueueSize.set(3000);

        rliTailSize = rateLimit.get(RateLimitName.BROKER_TAIL_SIZE.getName());
        rliTailSize.set(5);

        rliTps = rateLimit.get(RateLimitName.BROKER_TPS.getName());
        rateLimit.setActive(true);
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void setMaxSizeQueue(int newSize) {
        rliQueueSize.set(newSize);
    }

    public void setMaxSizeQueueTail(int newSize) {
        rliTailSize.set(newSize);
    }

    public void setMaxTpsInput(int maxTpsInput) {
        rliTps.set(maxTpsInput);
    }

    private void statistic(ExpirationMsImmutableEnvelope<TEO> envelope) {
        //#1, что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        timeInQueue.add(envelope.getInactivityTimeMs());
    }

    public DisposableExpirationMsImmutableEnvelope<TEO> add(TEO element, long curTime, long timeOut) throws Exception {
        return add(new ExpirationMsImmutableEnvelope<>(element, timeOut, curTime));
    }

    public DisposableExpirationMsImmutableEnvelope<TEO> add(TEO element, long timeOut) throws Exception {
        return add(new ExpirationMsImmutableEnvelope<>(element, timeOut));
    }

    @Override
    public DisposableExpirationMsImmutableEnvelope<TEO> add(ExpirationMsImmutableEnvelope<TEO> envelope) throws Exception {
        if (envelope == null) {
            throw new Exception("Element null");
        }
        DisposableExpirationMsImmutableEnvelope<TEO> convert = DisposableExpirationMsImmutableEnvelope.convert(envelope);
        if (!rliTps.check(null)) {
            throw new Exception(getExceptionInformation("RateLimitTps", envelope));
        }
        if (cyclical) {
            if (!rliQueueSize.check(queue.size() + 1)) {
                // Он конечно протух не по своей воле, но что делать...
                // Как будто лучше его закинуть по стандартной цепочке, что бы операция была завершена
                onDrop(queue.removeFirst());
            }
        } else {
            if (!rliQueueSize.check(queue.size())) {
                throw new Exception(getExceptionInformation("RateLimitSize", envelope));
            }
        }
        if (convert.isExpired()) {
            throw new Exception(getExceptionInformation("Expired", envelope));
        }
        queue.add(convert);
        tail.add(envelope);
        if (!rliTailSize.check(tail.size())) {
            tail.pollFirst();
        }
        return convert;
    }

    public ExpirationMsImmutableEnvelope<TEO> pollFirst() {
        return pool(true);
    }

    public ExpirationMsImmutableEnvelope<TEO> pollLast() {
        return pool(false);
    }

    private ExpirationMsImmutableEnvelope<TEO> pool(boolean first) {
        do {
            DisposableExpirationMsImmutableEnvelope<TEO> result = first ? queue.pollFirst() : queue.pollLast();
            if (result == null) {
                return null;
            }
            statistic(result);
            if (result.isExpired()) {
                onDrop(result);
                Thread.onSpinWait();
                continue;
            }
            TEO value = result.getValue();
            if (value == null) {
                Thread.onSpinWait();
                continue;
            }
            return result.revert();
        } while (!queue.isEmpty());
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void remove(DisposableExpirationMsImmutableEnvelope<TEO> envelope) {
        if (envelope != null) {
            statistic(envelope);
            queue.remove(envelope);
            // Делаем так, что бы он больше не достался никому
            envelope.getValue();
        }
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    String getExceptionInformation(String cause, ExpirationMsImmutableEnvelope<TEO> envelope) {
        StringBuilder sb = new StringBuilder()
                .append("Cause: ").append(cause).append("; ")
                .append("Max tps: ").append(rliTps.get()).append("; ")
                .append("Limit size: ").append(rliQueueSize.get()).append("; ")
                .append("Class add: ").append(envelope.getValue().getClass().getName()).append("; ")
                .append("Object add: ").append(envelope.getValue().toString()).append("; ");
        return sb.toString();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        UtilRisc.forEach(isThreadRun, queue, (DisposableExpirationMsImmutableEnvelope<TEO> envelope) -> {
            if (envelope.isExpired()) {
                queue.remove(envelope);
                onDrop(envelope);
            }
        });
    }

    //Обработка выпадающих сообщений
    public void onDrop(DisposableExpirationMsImmutableEnvelope<TEO> envelope) {
        if (envelope == null || onDrop == null) {
            return;
        }
        TEO value = envelope.getValue();
        if (value != null) {
            onDrop.accept(value);
        }
    }

    // Получить процент заполненности очереди
    public int getOccupancyPercentage() {
        //  MAX - 100
        //  500 - x
        return queue.size() * 100 / (int) rliQueueSize.get();
    }

    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        int tpsDequeueFlush = tpsDequeue.getAndSet(0);
        int sizeFlush = queue.size();
        if (sizeFlush > 0 || tpsDequeueFlush > 0) {
            active();
        }
        result.add(new Statistic(parentTags, parentFields)
                .addField("tpsDeq", tpsDequeueFlush)
                .addField("size", sizeFlush)
                .addFields(timeInQueue.flush("time"))
        );
        return result;
    }

    @Override
    public void close() {
        rateLimit.setActive(false);
    }

    // Рекомендуется использовать только для тестов
    public void reset() {
        queue.clear();
        tail.clear();
        tpsDequeue.set(0);
        cyclical = true;
        rateLimit.reset();
        rliTailSize.set(5);
        rliQueueSize.set(3000);
    }

    // Отладочная

    public List<TEO> getTail(@Nullable AtomicBoolean isRun) {
        final List<TEO> ret = new ArrayList<>();
        UtilRisc.forEach(isRun, tail, (ExpirationMsImmutableEnvelope<TEO> envelope) ->
                ret.add(envelope.getValue()));
        return ret;
    }

    public List<TEO> getCloneQueue(@Nullable AtomicBoolean isRun) {
        final List<TEO> cloned = new ArrayList<>();
        UtilRisc.forEach(isRun, queue, (DisposableExpirationMsImmutableEnvelope<TEO> envelope)
                -> cloned.add(envelope.revert().getValue()));
        return cloned;
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
        return this.classItem.equals(classItem);
    }

}
