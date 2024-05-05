package ru.jamsys.core.extension.addable;

@SuppressWarnings("unused")
public interface AddToList<T, R> {
    R add(T obj) throws Exception;
}
