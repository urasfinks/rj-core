package ru.jamsys.broker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BrokerQueue<T> {

    private final ConcurrentLinkedDeque<ElementWrap<T>> queue = new ConcurrentLinkedDeque<>();
    private final AtomicInteger tpsInput = new AtomicInteger(0);
    private final AtomicInteger tpsOutput = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Long> timeAvg = new ConcurrentLinkedQueue<>();
    private int limit = -1;

    public void add(T o) {
        tpsInput.incrementAndGet();
        queue.add(new ElementWrap<>(o));
        if (limit > 0 && queue.size() > limit) {
            ElementWrap<T> tElementWrap = queue.pollFirst();
            new Exception("Limit BrokerQueue: " + tElementWrap.getElement().getClass().getSimpleName() + "; Remove first element: " + tElementWrap.getElement().toString() + "; created: " + tElementWrap.getTimestamp()).printStackTrace();
        }
    }

    private T stat(ElementWrap<T> tElementWrap) {
        if (tElementWrap != null) {
            tpsOutput.incrementAndGet();
            timeAvg.add(System.currentTimeMillis() - tElementWrap.getTimestamp());
            return tElementWrap.getElement();
        }
        return null;
    }

    public T pollFirst() {
        return stat(queue.pollFirst());
    }

    public T pollLast() {
        return stat(queue.pollFirst());
    }


    public BrokerQueueStatistic flushStatistic() {
        int avgRes = 0;
        try { //Ловим модификатор, пока ни разу не ловил, на всякий случай
            double avg = timeAvg.stream().mapToLong(Long::intValue).summaryStatistics().getAverage();
            avgRes = (int) avg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BrokerQueueStatistic(getClass().getSimpleName(), tpsInput.getAndSet(0), tpsOutput.getAndSet(0), queue.size(), avgRes);
    }

    @SuppressWarnings({"unused", "unchecked"})
    public List<ElementWrap<T>> getCloneQueue() {
        Object[] objects = queue.toArray();
        List<ElementWrap<T>> ret = new ArrayList<>();
        for (Object o : objects) {
            ret.add((ElementWrap<T>) o);
        }
        return ret;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

}
