package ru.jamsys.core.component.item;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.component.api.RateLimitManager;
import ru.jamsys.core.extension.addable.AddableCollectionItem;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.IgnoreClassFinder;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitType;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.TimeControllerMsImpl;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Раньше на вставку был просто объект TEO и внутри происходила обёртка TimeEnvelope<TEO>
// Но потом пришла реализация Cache где нельзя было такое сделать
// и на вход надо было уже подавать объект TimeEnvelope<TEO>
// Я захотел, что бы везде было одинаково только лишь поэтому TEO -> TimeEnvelope<TEO>

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Getter
@Setter
@IgnoreClassFinder
public class Broker<TEO>
        extends TimeControllerMsImpl
        implements
        StatisticsFlush,
        Closable,
        AddableCollectionItem<
                TimeEnvelopeMs<TEO>,
                TimeEnvelopeMs<TEO>
                > {

    private final ConcurrentLinkedDeque<TimeEnvelopeMs<TEO>> queue = new ConcurrentLinkedDeque<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<TimeEnvelopeMs<TEO>> tail = new ConcurrentLinkedDeque<>();

    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    private int sizeQueue = 3000;

    private int sizeTail = 5;

    private boolean cyclical = true;

    public int getSize() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    final RateLimit rateLimit;

    final RateLimitItem rateLimitSize;

    final RateLimitItem rateLimitTps;

    public Broker(String key) {
        rateLimit = App.context.getBean(RateLimitManager.class).get(getClass(), key);
        rateLimitSize = rateLimit.get(RateLimitType.BROKER_SIZE);
        rateLimitSize.setMax(sizeQueue);
        rateLimitTps = rateLimit.get(RateLimitType.BROKER_TPS);
        rateLimit.setActive(true);
    }

    public void setSizeQueue(int newSize) {
        sizeQueue = newSize;
        rateLimitSize.setMax(newSize);
    }

    private void statistic(TimeEnvelopeMs<TEO> timeEnvelopeMs) {
        //#1, что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        if (timeEnvelopeMs != null) {
            timeInQueue.add(timeEnvelopeMs.getOffsetLastActivityMs());
        }
    }

    public TimeEnvelopeMs<TEO> pollFirst() {
        TimeEnvelopeMs<TEO> result = queue.pollFirst();
        statistic(result);
        return result;
    }

    public TimeEnvelopeMs<TEO> pollLast() {
        TimeEnvelopeMs<TEO> result = queue.pollLast();
        statistic(result);
        return result;
    }

    public void remove(TimeEnvelopeMs<TEO> timeEnvelopeMs) {
        statistic(timeEnvelopeMs);
        queue.remove(timeEnvelopeMs);
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    @SuppressWarnings("unused")
    public List<TEO> getCloneQueue(@Nullable AtomicBoolean isRun) {
        List<TEO> cloned = new ArrayList<>();
        List<TimeEnvelopeMs<TEO>> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, queue, getEmptyType(), (TimeEnvelopeMs<TEO> elementEnvelope)
                -> cloned.add(elementEnvelope.getValue()));
        return cloned;
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        queue.clear();
        tail.clear();
        tpsDequeue.set(0);
        sizeQueue = 3000;
        sizeTail = 5;
        cyclical = true;
        rateLimit.reset();
    }

    public void setMaxTpsInput(int maxTpsInput) {
        rateLimitTps.setMax(maxTpsInput);
    }

    public List<TEO> getTail(@Nullable AtomicBoolean isRun) {
        List<TEO> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, tail, getEmptyType(), (TimeEnvelopeMs<TEO> elementEnvelope) -> ret.add(elementEnvelope.getValue()));
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

    public TimeEnvelopeMs<TEO> addTest(TEO element) throws Exception {
        return add(element, 6_000);
    }

    public TimeEnvelopeMs<TEO> add(TEO element, long curTime, long timeOut) throws Exception {
        TimeEnvelopeMs<TEO> timeEnvelopeMs = new TimeEnvelopeMs<>(element);
        timeEnvelopeMs.setLastActivityMs(curTime);
        timeEnvelopeMs.setKeepAliveOnInactivityMs(timeOut);
        return add(timeEnvelopeMs);
    }

    public TimeEnvelopeMs<TEO> add(TEO element, long timeOut) throws Exception {
        TimeEnvelopeMs<TEO> timeEnvelopeMs = new TimeEnvelopeMs<>(element);
        timeEnvelopeMs.setKeepAliveOnInactivityMs(timeOut);
        return add(timeEnvelopeMs);
    }

    @Override
    public TimeEnvelopeMs<TEO> add(TimeEnvelopeMs<TEO> timeEnvelopeMs) throws Exception {
        if (timeEnvelopeMs == null) {
            throw new Exception("Element null");
        }
        if (!rateLimitTps.check(null)) {
            throw new Exception(getExceptionInformation("RateLimitTps", timeEnvelopeMs));
        }
        if (cyclical) {
            if (!rateLimitSize.check(queue.size() + 1)) {
                queue.removeFirst();
            }
        } else {
            if (!rateLimitSize.check(queue.size())) {
                throw new Exception(getExceptionInformation("RateLimitSize", timeEnvelopeMs));
            }
        }
        if (timeEnvelopeMs.isExpired()) {
            throw new Exception(getExceptionInformation("Expired", timeEnvelopeMs));
        }
        queue.add(timeEnvelopeMs);
        tail.add(timeEnvelopeMs);
        if (tail.size() > sizeTail) {
            tail.pollFirst();
        }
        return timeEnvelopeMs;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    String getExceptionInformation(String cause, TimeEnvelopeMs<TEO> timeEnvelopeMs) {
        StringBuilder sb = new StringBuilder()
                .append("Cause: ").append(cause).append("; ")
                .append("Max tps: ").append(rateLimitTps.getMax()).append("; ")
                .append("Limit size: ").append(rateLimitSize.getMax()).append("; ")
                .append("Class add: ").append(timeEnvelopeMs.getValue().getClass().getSimpleName()).append("; ")
                .append("Object add: ").append(timeEnvelopeMs.getValue().toString()).append("; ");
        return sb.toString();
    }

}
