package ru.jamsys.broker;

import ru.jamsys.Util;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.BrokerQueueStatistic;
import ru.jamsys.statistic.Statistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class BrokerQueue<T> {

    private final ConcurrentLinkedDeque<ElementWrap<T>> queue = new ConcurrentLinkedDeque<>();
    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<T> tail = new ConcurrentLinkedDeque<>();
    private final AtomicInteger tpsInput = new AtomicInteger(0);
    private final AtomicInteger tpsOutput = new AtomicInteger(0);

    //private final ConcurrentLinkedQueue<Long> timeInQueue = new ConcurrentLinkedQueue<>();
    private final AvgMetric timeInQueue = new AvgMetric();

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

    private T stat(ElementWrap<T> elementWrap) {
        if (elementWrap != null) {
            tpsOutput.incrementAndGet();
            timeInQueue.add(System.currentTimeMillis() - elementWrap.getTimestamp());
            return elementWrap.getElement();
        }
        return null;
    }

    public T pollFirst() {
        return stat(queue.pollFirst());
    }

    public T pollLast() {
        return stat(queue.pollLast());
    }

    public Statistic flushAndGetStatistic() {
        return new BrokerQueueStatistic(
                tpsInput.getAndSet(0),
                tpsOutput.getAndSet(0),
                queue.size(),
                timeInQueue.flush()
        );
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    @SuppressWarnings("unused")
    public List<T> getCloneQueue() {
        List<T> ret = new ArrayList<>();
        Util.riskModifierCollection(queue, getEmptyType(), (ElementWrap<T> elementWrap) -> ret.add(elementWrap.getElement()));
        return ret;
    }

    public List<T> getTail() {
        List<T> ret = new ArrayList<>();
        Util.riskModifierCollection(tail, getEmptyType(), ret::add);
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
        // Не считаю, что данные в очереди должны быть очищены
        // Может быть их кто-то успеет ещё обработать?
    }

}
