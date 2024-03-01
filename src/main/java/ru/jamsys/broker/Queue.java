package ru.jamsys.broker;

import java.util.List;

public interface Queue<T> {
    int getSize();
    void add(T o) throws Exception;
    T pollFirst();
    T pollLast();
    List<T> getTail();
}
