package ru.jamsys.extension;

public interface AddableComponentItemList<T, R> {
    R add(T obj) throws Exception;
}
