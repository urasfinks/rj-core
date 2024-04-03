package ru.jamsys.broker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.extension.*;
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

@Getter
@Setter
@IgnoreClassFinder
public class BrokerQueue<T> extends TimeControllerImpl implements StatisticsCollector, Closable, Addable<T, TimeEnvelope<T>> {

    private final ConcurrentLinkedDeque<TimeEnvelope<T>> queue = new ConcurrentLinkedDeque<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<T> tail = new ConcurrentLinkedDeque<>();

    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    private final List<Procedure> listProcedure = new ArrayList<>();

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

    @SuppressWarnings("unused")
    public void onAdd(Procedure procedure) {
        listProcedure.add(procedure);
    }

    private void statistic(TimeEnvelope<T> timeEnvelope) {
        //#1 что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        if (timeEnvelope != null) {
            timeInQueue.add(timeEnvelope.getOffsetLastActivityMs());
        }
    }

    public T pollFirst() {
        TimeEnvelope<T> result = queue.pollFirst();
        statistic(result);
        if (result != null) {
            return result.getValue();
        }
        return null;
    }

    public T pollLast() {
        TimeEnvelope<T> result = queue.pollLast();
        statistic(result);
        if (result != null) {
            return result.getValue();
        }
        return null;
    }

    public void remove(TimeEnvelope<T> timeEnvelope) {
        statistic(timeEnvelope);
        if (timeEnvelope != null) {
            queue.remove(timeEnvelope);
        }
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    @SuppressWarnings("unused")
    public List<T> getCloneQueue(@Nullable AtomicBoolean isRun) {
        List<T> cloned = new ArrayList<>();
        List<TimeEnvelope<T>> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, queue, getEmptyType(), (TimeEnvelope<T> elementEnvelope)
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

    public List<T> getTail(@Nullable AtomicBoolean isRun) {
        List<T> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, tail, getEmptyType(), ret::add);
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

    @Override
    public TimeEnvelope<T> add(T element) throws Exception {
        if (element == null) {
            throw new Exception("Element null");
        }
        if (!rateLimitTps.check(null)) {
            throw new Exception("RateLimit BrokerQueue: " + element.getClass().getSimpleName() + "; max tps: " + rateLimitTps.getMax() + "; object: " + element);
        }
        if (cyclical) {
            if (!rateLimitSize.check(queue.size() + 1)) {
                queue.removeFirst();
            }
        } else {
            if (!rateLimitSize.check(queue.size())) {
                throw new Exception("Limit BrokerQueue: " + element.getClass().getSimpleName() + "; limit: " + rateLimitSize.getMax() + "; object: " + element);
            }
        }
        TimeEnvelope<T> result = new TimeEnvelope<>(element);
        queue.add(result);
        tail.add(element);
        if (tail.size() > sizeTail) {
            tail.pollFirst();
        }
        try {
            listProcedure.forEach(Procedure::run);
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        return result;
    }

}
