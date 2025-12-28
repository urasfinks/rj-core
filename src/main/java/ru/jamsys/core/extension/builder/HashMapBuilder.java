package ru.jamsys.core.extension.builder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HashMapBuilder<K, V> extends LinkedHashMap<K, V> {

    public HashMapBuilder() {
    }

    public HashMapBuilder(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public HashMapBuilder<K, V> append(K key, V value) {
        put(key, value);
        return this;
    }

    public HashMapBuilder<K, V> apply(Consumer<HashMapBuilder<K, V>> consumer) {
        consumer.accept(this);
        return this;
    }

    public HashMapBuilder<K, V> appendIfNotNull(K key, V value) {
        if (value != null) {
            put(key, value);
        }
        return this;
    }

    public HashMapBuilder<K, V> appendIfNonZero(K key, int value) {
        if (value != 0) {
            @SuppressWarnings("unchecked")
            V o = (V) Integer.valueOf(value);
            put(key, o);
        }
        return this;
    }

}
