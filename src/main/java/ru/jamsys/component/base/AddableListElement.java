package ru.jamsys.component.base;

public interface AddableListElement<T, R> {
    R add(T obj) throws Exception;
}
