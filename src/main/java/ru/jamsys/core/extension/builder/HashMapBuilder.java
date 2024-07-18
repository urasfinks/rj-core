package ru.jamsys.core.extension.builder;

import java.util.HashMap;

public class HashMapBuilder<K, V> extends HashMap<K, V> {

    public HashMapBuilder<K, V> append(K key, V value) {
        put(key, value);
        return this;
    }

}
