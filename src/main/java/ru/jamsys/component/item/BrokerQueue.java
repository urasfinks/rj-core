package ru.jamsys.component.item;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.App;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.component.general.addable.AddableCollectionItem;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.IgnoreClassFinder;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.rate.limit.RateLimit;
import ru.jamsys.rate.limit.RateLimitName;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TimeControllerImpl;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Раньше на вставку был просто объект TEO и внутри происходила обёртка TimeEnvelope<TEO>
// Но потом пришла реализация  Cache  где нельзя было такое сделать
// и на вход надо было уже подавать объект TimeEnvelope<TEO>
// Я захотел, что бы везде было одинаково только лишь поэтому TEO -> TimeEnvelope<TEO>

@Getter
@Setter
@IgnoreClassFinder
public class BrokerQueue<TEO>
        extends TimeControllerImpl
        implements
        StatisticsCollector,
        Closable,
        AddableCollectionItem<
                TimeEnvelope<TEO>,
                TimeEnvelope<TEO>
                > {

    private final ConcurrentLinkedDeque<TimeEnvelope<TEO>> queue = new ConcurrentLinkedDeque<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<TimeEnvelope<TEO>> tail = new ConcurrentLinkedDeque<>();

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

    public BrokerQueue(String key) {
        rateLimit = App.context.getBean(RateLimitManager.class).get(getClass(), key);
        rateLimitSize = rateLimit.get(RateLimitName.BROKER_SIZE);
        rateLimitSize.setMax(sizeQueue);
        rateLimitTps = rateLimit.get(RateLimitName.BROKER_TPS);
        rateLimit.setActive(true);
    }

    public void setSizeQueue(int newSize) {
        sizeQueue = newSize;
        rateLimitSize.setMax(newSize);
    }

    private void statistic(TimeEnvelope<TEO> timeEnvelope) {
        //#1 что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        if (timeEnvelope != null) {
            timeInQueue.add(timeEnvelope.getOffsetLastActivityMs());
        }
    }

    public TimeEnvelope<TEO> pollFirst() {
        TimeEnvelope<TEO> result = queue.pollFirst();
        statistic(result);
        return result;
    }

    public TimeEnvelope<TEO> pollLast() {
        TimeEnvelope<TEO> result = queue.pollLast();
        statistic(result);
        return result;
    }

    public void remove(TimeEnvelope<TEO> timeEnvelope) {
        statistic(timeEnvelope);
        queue.remove(timeEnvelope);
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    @SuppressWarnings("unused")
    public List<TEO> getCloneQueue(@Nullable AtomicBoolean isRun) {
        List<TEO> cloned = new ArrayList<>();
        List<TimeEnvelope<TEO>> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, queue, getEmptyType(), (TimeEnvelope<TEO> elementEnvelope)
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
        Util.riskModifierCollection(isRun, tail, getEmptyType(), (TimeEnvelope<TEO> elementEnvelope) -> ret.add(elementEnvelope.getValue()));
        return ret;
    }

    @SuppressWarnings("unused")
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope) {
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

    public TimeEnvelope<TEO> addTest(TEO element) throws Exception {
        return add(element, 6_000);
    }

    public TimeEnvelope<TEO> add(TEO element, long curTime, long timeOut) throws Exception {
        TimeEnvelope<TEO> timeEnvelope = new TimeEnvelope<>(element);
        timeEnvelope.setLastActivity(curTime);
        timeEnvelope.setKeepAliveOnInactivityMs(timeOut);
        return add(timeEnvelope);
    }

    public TimeEnvelope<TEO> add(TEO element, long timeOut) throws Exception {
        TimeEnvelope<TEO> timeEnvelope = new TimeEnvelope<>(element);
        timeEnvelope.setKeepAliveOnInactivityMs(timeOut);
        return add(timeEnvelope);
    }

    @Override
    public TimeEnvelope<TEO> add(TimeEnvelope<TEO> timeEnvelope) throws Exception {
        if (timeEnvelope == null) {
            throw new Exception("Element null");
        }
        if (!rateLimitTps.check(null)) {
            throw new Exception("RateLimit BrokerQueue: " + timeEnvelope.getValue().getClass().getSimpleName() + "; max tps: " + rateLimitTps.getMax() + "; object: " + timeEnvelope);
        }
        if (cyclical) {
            if (!rateLimitSize.check(queue.size() + 1)) {
                queue.removeFirst();
            }
        } else {
            if (!rateLimitSize.check(queue.size())) {
                throw new Exception("Limit BrokerQueue: " + timeEnvelope.getValue().getClass().getSimpleName() + "; limit: " + rateLimitSize.getMax() + "; object: " + timeEnvelope);
            }
        }
        queue.add(timeEnvelope);
        tail.add(timeEnvelope);
        if (tail.size() > sizeTail) {
            tail.pollFirst();
        }
        return timeEnvelope;
    }

}
