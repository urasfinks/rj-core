package ru.jamsys.core.extension.raw.writer;

public interface ResourceQueue<T> extends Closable {

    long size();

    T pollFirst();

    T pollLast();

    void add(T r) throws Exception;

}
