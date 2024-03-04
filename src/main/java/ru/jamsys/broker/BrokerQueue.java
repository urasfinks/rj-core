package ru.jamsys.broker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticsCollector;
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
public class BrokerQueue<T> implements Queue<T>, StatisticsCollector {

    private final ConcurrentLinkedDeque<T> queue = new ConcurrentLinkedDeque<>();
    private final Map<T, Long> timing = new ConcurrentHashMap<>();
    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<T> tail = new ConcurrentLinkedDeque<>();
    private final AtomicInteger tpsInput = new AtomicInteger(0);
    private final AtomicInteger tpsOutput = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    private int sizeQueue = 3000;
    private int sizeTail = 5;
    private boolean cyclical = true;

    public int getSize() {
        return queue.size();
    }

    @Override
    public void add(T o) throws Exception {
        if (cyclical) {
            if (queue.size() >= sizeQueue) {
                queue.removeFirst();
            }
        } else {
            if (queue.size() > sizeQueue) {
                throw new Exception("Limit BrokerQueue: " + o.getClass().getSimpleName() + "; limit: " + sizeQueue + "; object: " + o);
            }
        }
        tpsInput.incrementAndGet();
        timing.put(o, System.currentTimeMillis());
        queue.add(o);
        tail.add(o);
        if (tail.size() > sizeTail) {
            tail.pollFirst();
        }
    }

    private void statistic(T o) {
        if (o != null) {
            tpsOutput.incrementAndGet();
            Long removeMs = timing.remove(o);
            if (removeMs != null) {
                timeInQueue.add(System.currentTimeMillis() - removeMs);
            } else {
                App.context.getBean(ExceptionHandler.class).handler(new RuntimeException("Object not found in the timing map. This is a serious problem with your implementation"));
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
        queue.clear();
        timing.clear();
        tail.clear();
        tpsInput.set(0);
        tpsOutput.set(0);
        sizeQueue = 3000;
        sizeTail = 5;
        cyclical = true;
    }

    @Override
    public List<T> getTail(@Nullable AtomicBoolean isRun) {
        List<T> ret = new ArrayList<>();
        Util.riskModifierCollection(isRun, tail, getEmptyType(), ret::add);
        return ret;
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("tpsInput", tpsInput.getAndSet(0))
                .addField("tpsOutput", tpsOutput.getAndSet(0))
                .addField("size", queue.size())
                .addFields(timeInQueue.flush("timeMsInQueue"))
        );
        return result;
    }

}
