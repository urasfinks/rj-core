package ru.jamsys.broker;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Queue<T> {

    int getSize();

    boolean isEmpty();

    QueueElementEnvelope<T> add(T element) throws Exception;

    T pollFirst();

    T pollLast();

    void remove(QueueElementEnvelope<T> queueElementEnvelope);

    List<T> getTail(@Nullable AtomicBoolean isRun);

    List<T> getCloneQueue(@Nullable AtomicBoolean isRun);

    void reset();

    void setMaxTpsInput(int maxTpsInput);

    boolean isExpired();

    void close();

}
