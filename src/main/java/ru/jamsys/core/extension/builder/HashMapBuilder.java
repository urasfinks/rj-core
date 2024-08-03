package ru.jamsys.core.extension.builder;

import java.util.LinkedHashMap;

public class HashMapBuilder<K, V> extends LinkedHashMap<K, V> {

    public HashMapBuilder<K, V> append(K key, V value) {
        put(key, value);
        return this;
    }

}
