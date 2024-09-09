package ru.jamsys.core.extension.builder;

import java.util.LinkedHashSet;
import java.util.Set;

public class HashSetBuilder<V> extends LinkedHashSet<V> {

    public HashSetBuilder() {
    }

    public HashSetBuilder(Set<? extends V> m) {
        super(m);
    }

    public HashSetBuilder<V> append(V value) {
        add(value);
        return this;
    }

}
