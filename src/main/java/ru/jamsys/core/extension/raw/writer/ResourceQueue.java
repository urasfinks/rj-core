package ru.jamsys.core.extension.raw.writer;

public interface ResourceQueue<T> extends Closable {

    long size();

    T readFirst();

    T readLast();

    void writeFirst(T r);

    void writeLast(T r);

}
