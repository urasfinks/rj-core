package ru.jamsys.core.extension.addable;

@SuppressWarnings("unused")
public interface AddableCollectionItem<T, R> {
    R add(T obj) throws Exception;
}
