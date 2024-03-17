package ru.jamsys.broker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.extension.IgnoreClassFinder;
import ru.jamsys.extension.Procedure;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@IgnoreClassFinder
public class BrokerQueue<T> implements Queue<T>, StatisticsCollector {

    private final ConcurrentLinkedDeque<T> queue = new ConcurrentLinkedDeque<>();

    private final Map<T, Long> timing = new ConcurrentHashMap<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<T> tail = new ConcurrentLinkedDeque<>();

    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    private final List<Procedure> listProcedure = new ArrayList<>();

    private volatile long lastActivity = 0;

    private int sizeQueue = 3000;

    private long keepAliveOnInactivityMs = 60_000; // Время жизни очереди, если в ней нет активности

    private int sizeTail = 5;

    private boolean cyclical = true;

    public int getSize() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    final RateLimitItem rateLimitItem;

    public BrokerQueue(RateLimitItem rateLimitItem) {
        this.rateLimitItem = rateLimitItem;
    }

    @SuppressWarnings("unused")
    public void onAdd(Procedure procedure) {
        listProcedure.add(procedure);
    }

    @Override
    public void add(T o) throws Exception {
        if (!rateLimitItem.check()) {
            throw new Exception("RateLimit BrokerQueue: " + o.getClass().getSimpleName() + "; max tps: " + rateLimitItem.getMaxTps() + "; object: " + o);
        }
        if (cyclical) {
            if (queue.size() >= sizeQueue) {
                queue.removeFirst();
            }
        } else {
            if (queue.size() > sizeQueue) {
                throw new Exception("Limit BrokerQueue: " + o.getClass().getSimpleName() + "; limit: " + sizeQueue + "; object: " + o);
            }
        }
        timing.put(o, System.currentTimeMillis());
        queue.add(o);
        tail.add(o);
        if (tail.size() > sizeTail) {
            tail.pollFirst();
        }
        try {
            listProcedure.forEach(Procedure::run);
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    private void statistic(T o) {
        //#1 что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        if (o != null) {
            Long removeMs = timing.remove(o);
            if (removeMs != null) {
                timeInQueue.add(System.currentTimeMillis() - removeMs);
            } else {
                App.context.getBean(ExceptionHandler.class).handler(new RuntimeException("Object not found in the timing map"));
            }
        }
    }

    public T pollFirst() {
        T result = queue.pollFirst();
        statistic(result);
        return result;
    }

    public T pollLast() {
        T result = queue.pollLast();
        statistic(result);
        return result;
    }

    @Override
    public void remove(T object) {
        statistic(object);
        queue.remove(object);
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    @SuppressWarnings("unused")
    @Override
    public List<T> getCloneQueue(@Nullable AtomicBoolean isRun) {
        List<T> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, queue, getEmptyType(), ret::add);
        return ret;
    }

    @Override
    public void reset() {
        // Рекомендуется использовать только для тестов
        queue.clear();
        timing.clear();
        tail.clear();
        tpsDequeue.set(0);
        sizeQueue = 3000;
        sizeTail = 5;
        cyclical = true;
        rateLimitItem.reset();
    }

    @Override
    public void setMaxTpsInput(int maxTpsInput) {
        rateLimitItem.setMaxTps(maxTpsInput);
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() > lastActivity + keepAliveOnInactivityMs;
    }

    @Override
    public List<T> getTail(@Nullable AtomicBoolean isRun) {
        List<T> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, tail, getEmptyType(), ret::add);
        return ret;
    }

    @SuppressWarnings("unused")
    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        int tpsDequeueFlush = tpsDequeue.getAndSet(0);
        int sizeFlush = queue.size();
        if (sizeFlush > 0 || tpsDequeueFlush > 0) {
            lastActivity = System.currentTimeMillis();
        }
        result.add(new Statistic(parentTags, parentFields)
                .addField("tpsDequeue", tpsDequeueFlush)
                .addField("size", sizeFlush)
                .addFields(timeInQueue.flush("timeMsInQueue"))
        );
        return result;
    }

}
