package ru.jamsys.component.general.addable;

public interface AddableMapItem<K, V, R> {
    R add(K key, V value) throws Exception;
}
