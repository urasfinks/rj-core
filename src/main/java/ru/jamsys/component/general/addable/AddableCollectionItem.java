package ru.jamsys.component.general.addable;

public interface AddableCollectionItem<T, R> {
    R add(T obj) throws Exception;
}
