package ru.jamsys.extension;

public interface Addable<T, R> {
    R add(T obj) throws Exception;
}
