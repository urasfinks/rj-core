package ru.jamsys.component.base;

public interface AddableMapElement<K, V, R> {
    R add(K key, V value) throws Exception;
}
