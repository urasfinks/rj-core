package ru.jamsys.broker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BrokerQueue<T> {

    private final ConcurrentLinkedDeque<ElementWrap<T>> queue = new ConcurrentLinkedDeque<>();
    //Последнии сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<T> tail = new ConcurrentLinkedDeque<>();
    private final AtomicInteger tpsInput = new AtomicInteger(0);
    private final AtomicInteger tpsOutput = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Long> timeInQueue = new ConcurrentLinkedQueue<>();
    private int sizeQueue = 3000;
    private int sizeTail = 5;
    private boolean cyclical = true;

    public int getSize() {
        return queue.size();
    }

    public void add(T o) throws Exception {
        tpsInput.incrementAndGet();
        if (cyclical) {
            queue.add(new ElementWrap<>(o));
            if (queue.size() > sizeQueue) {
                queue.removeFirst();
            }
        } else {
            if (queue.size() > sizeQueue) {
                throw new Exception("Limit BrokerQueue: " + o.getClass().getSimpleName() + "; limit: " + sizeQueue + "; object: " + o);
            }
            queue.add(new ElementWrap<>(o));
        }
        tail.add(o);
        if (tail.size() > sizeTail) {
            tail.pollFirst();
        }
    }

    private T stat(ElementWrap<T> tElementWrap) {
        if (tElementWrap != null) {
            tpsOutput.incrementAndGet();
            timeInQueue.add(System.currentTimeMillis() - tElementWrap.getTimestamp());
            return tElementWrap.getElement();
        }
        return null;
    }

    public T pollFirst() {
        return stat(queue.pollFirst());
    }

    public T pollLast() {
        return stat(queue.pollLast());
    }

    public BrokerQueueStatistic flushStatistic() {
        int avgTimeInQueue = 0;
        try { //Ловим модификатор, пока ни разу не ловил, на всякий случай
            double avg = timeInQueue.stream().mapToLong(Long::intValue).summaryStatistics().getAverage();
            avgTimeInQueue = (int) avg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BrokerQueueStatistic(getClass().getSimpleName(), tpsInput.getAndSet(0), tpsOutput.getAndSet(0), queue.size(), avgTimeInQueue);
    }

    @SuppressWarnings("unused")
    public List<T> getCloneQueue() {
        Object[] objects = queue.toArray();
        List<T> ret = new ArrayList<>();
        for (Object o : objects) {
            ret.add(((ElementWrap<T>) o).getElement());
        }
        return ret;
    }

    public List<T> getTail() {
        Object[] objects = tail.toArray();
        List<T> ret = new ArrayList<>();
        for (Object o : objects) {
            ret.add((T) o);
        }
        return ret;
    }

    public void setSizeQueue(int sizeQueue) {
        this.sizeQueue = sizeQueue;
    }

    public void setSizeTail(int sizeTail) {
        this.sizeTail = sizeTail;
    }

    public void setCyclical(boolean cyclical) {
        this.cyclical = cyclical;
    }

    public void shutdown() {
        queue.clear();
        tail.clear();
    }

}
