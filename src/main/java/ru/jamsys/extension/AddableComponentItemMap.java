package ru.jamsys.extension;

public interface AddableComponentItemMap<K, V, R> {
    R add(K key, V value) throws Exception;
}
