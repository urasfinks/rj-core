package ru.jamsys.core.extension.raw.writer;

// Нечто, что закончилось

public interface Completed<E> {

    boolean isCompleted();

    E getIfNotCompleted();

    void release();

}
