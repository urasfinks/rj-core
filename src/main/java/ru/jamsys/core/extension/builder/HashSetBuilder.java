package ru.jamsys.core.extension.builder;

import java.util.LinkedHashSet;

public class HashSetBuilder<V> extends LinkedHashSet<V> {
    public HashSetBuilder<V> append(V value) {
        add(value);
        return this;
    }
}
