package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.manager.RateLimitManager;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;
import ru.jamsys.core.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Раньше на вставку был просто объект TEO и внутри происходила обёртка TimeEnvelope<TEO>
// Но потом пришла реализация Cache где нельзя было такое сделать
// и на вход надо было уже подавать объект TimeEnvelope<TEO>
//     Причина: там надо сначала выставлять время, на которое будем кешировать, а только потом делать вставку
//              Индексу рассчитывается из времени протухания
//              После установки время протухания изменить его было нельзя, так как индекс уже был зафиксирован
// Я захотел, что бы везде было одинаково. Только лишь поэтому TEO -> TimeEnvelope<TEO>
// 11.05.2024 Cache перехал в Session, и вся история с однотипным протоколом распалась
// Но пришёл на замену ExpirationManager и сейчас буду переделывать

//TODO: сделать TEO наследуемым от getIndex (RqUID)
@SuppressWarnings({"unused", "UnusedReturnValue"})
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
        AddToList<
                ExpirationMsImmutableEnvelope<TEO>,
                ExpirationMsImmutableEnvelope<TEO>
                > {

    private final ConcurrentLinkedDeque<ExpirationMsImmutableEnvelope<TEO>> queue = new ConcurrentLinkedDeque<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<ExpirationMsImmutableEnvelope<TEO>> tail = new ConcurrentLinkedDeque<>();

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    private boolean cyclical = true;

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    final RateLimit rateLimit;

    final RateLimitItem rliQueueSize;

    final RateLimitItem rliTailSize;

    final RateLimitItem rliTps;

    final String index;

    public Broker(String index, ApplicationContext applicationContext) {
        this.index = index;

        rateLimit = applicationContext.getBean(RateLimitManager.class).get(getClassName(index, applicationContext))
                .init(RateLimitName.BROKER_SIZE.getName(), RateLimitItemInstance.MAX)
                .init(RateLimitName.BROKER_TAIL_SIZE.getName(), RateLimitItemInstance.MAX)
                .init(RateLimitName.BROKER_TPS.getName(), RateLimitItemInstance.TPS);

        rliQueueSize = rateLimit.get(RateLimitName.BROKER_SIZE.getName());
        rliQueueSize.setMax(3000);

        rliTailSize = rateLimit.get(RateLimitName.BROKER_TAIL_SIZE.getName());
        rliTailSize.setMax(5);

        rliTps = rateLimit.get(RateLimitName.BROKER_TPS.getName());
        rateLimit.setActive(true);
    }

    public void setSizeQueue(int newSize) {
        rliQueueSize.setMax(newSize);
    }

    public void setSizeQueueTail(int newSize) {
        rliTailSize.setMax(newSize);
    }

    private void statistic(ExpirationMsImmutableEnvelope<TEO> ExpirationMsImmutableEnvelope) {
        //#1, что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        timeInQueue.add(ExpirationMsImmutableEnvelope.getInactivityTimeMs());
    }

    // Получить процент заполненности очереди
    public int getOccupancyPercentage() {
        /*  MAX - 100
            500 - x    */
        return queue.size() * 100 / (int) rliQueueSize.getMax();
    }

    public ExpirationMsImmutableEnvelope<TEO> pollFirst() {
        do {
            ExpirationMsImmutableEnvelope<TEO> result = queue.pollFirst();
            if (result == null) {
                return null;
            }
            statistic(result);
            if (result.isExpired()) {
                Thread.onSpinWait();
                continue;
            }
            return result;
        } while (!queue.isEmpty());
        return null;
    }

    public ExpirationMsImmutableEnvelope<TEO> pollLast() {
        do {
            ExpirationMsImmutableEnvelope<TEO> result = queue.pollLast();
            if (result == null) {
                return null;
            }
            statistic(result);
            if (result.isExpired()) {
                Thread.onSpinWait();
                continue;
            }
            return result;
        } while (!queue.isEmpty());
        return null;
    }

    public void remove(ExpirationMsImmutableEnvelope<TEO> ExpirationMsImmutableEnvelope) {
        if (ExpirationMsImmutableEnvelope != null) {
            statistic(ExpirationMsImmutableEnvelope);
            queue.remove(ExpirationMsImmutableEnvelope);
        }
    }

    @SuppressWarnings("unused")
    public List<TEO> getCloneQueue(@Nullable AtomicBoolean isRun) {
        List<TEO> cloned = new ArrayList<>();
        List<ExpirationMsImmutableEnvelope<TEO>> ret = new ArrayList<>();
        UtilRisc.forEach(isRun, queue, (ExpirationMsImmutableEnvelope<TEO> elementEnvelope)
                -> cloned.add(elementEnvelope.getValue()));
        return cloned;
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        queue.clear();
        tail.clear();
        tpsDequeue.set(0);
        cyclical = true;
        rateLimit.reset();
        rliTailSize.setMax(5);
        rliQueueSize.setMax(3000);
    }

    public void setMaxTpsInput(int maxTpsInput) {
        rliTps.setMax(maxTpsInput);
    }

    public List<TEO> getTail(@Nullable AtomicBoolean isRun) {
        List<TEO> ret = new ArrayList<>();
        UtilRisc.forEach(isRun, tail, (ExpirationMsImmutableEnvelope<TEO> elementEnvelope) -> ret.add(elementEnvelope.getValue()));
        return ret;
    }

    @SuppressWarnings("unused")
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

    public ExpirationMsImmutableEnvelope<TEO> add(TEO element, long curTime, long timeOut) throws Exception {
        ExpirationMsImmutableEnvelope<TEO> ExpirationMsImmutableEnvelope = new ExpirationMsImmutableEnvelope<>(element, timeOut, curTime);
        return add(ExpirationMsImmutableEnvelope);
    }

    public ExpirationMsImmutableEnvelope<TEO> add(TEO element, long timeOut) throws Exception {
        ExpirationMsImmutableEnvelope<TEO> ExpirationMsImmutableEnvelope = new ExpirationMsImmutableEnvelope<>(element, timeOut);
        return add(ExpirationMsImmutableEnvelope);
    }

    @Override
    public ExpirationMsImmutableEnvelope<TEO> add(ExpirationMsImmutableEnvelope<TEO> ExpirationMsImmutableEnvelope) throws Exception {
        if (ExpirationMsImmutableEnvelope == null) {
            throw new Exception("Element null");
        }
        if (!rliTps.check(null)) {
            throw new Exception(getExceptionInformation("RateLimitTps", ExpirationMsImmutableEnvelope));
        }
        if (cyclical) {
            if (!rliQueueSize.check(queue.size() + 1)) {
                queue.removeFirst();
            }
        } else {
            if (!rliQueueSize.check(queue.size())) {
                throw new Exception(getExceptionInformation("RateLimitSize", ExpirationMsImmutableEnvelope));
            }
        }
        if (ExpirationMsImmutableEnvelope.isExpired()) {
            throw new Exception(getExceptionInformation("Expired", ExpirationMsImmutableEnvelope));
        }
        queue.add(ExpirationMsImmutableEnvelope);
        tail.add(ExpirationMsImmutableEnvelope);
        if (!rliTailSize.check(tail.size())) {
            tail.pollFirst();
        }
        return ExpirationMsImmutableEnvelope;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    String getExceptionInformation(String cause, ExpirationMsImmutableEnvelope<TEO> ExpirationMsImmutableEnvelope) {
        StringBuilder sb = new StringBuilder()
                .append("Cause: ").append(cause).append("; ")
                .append("Max tps: ").append(rliTps.getMax()).append("; ")
                .append("Limit size: ").append(rliQueueSize.getMax()).append("; ")
                .append("Class add: ").append(ExpirationMsImmutableEnvelope.getValue().getClass().getName()).append("; ")
                .append("Object add: ").append(ExpirationMsImmutableEnvelope.getValue().toString()).append("; ");
        return sb.toString();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        UtilRisc.forEach(isThreadRun, queue, (ExpirationMsImmutableEnvelope<TEO> teoExpirationMsImmutableEnvelope) -> {
            if (teoExpirationMsImmutableEnvelope.isExpired()) {
                queue.remove(teoExpirationMsImmutableEnvelope);
            }
        });
    }
}
