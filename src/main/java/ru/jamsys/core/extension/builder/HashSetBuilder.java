package ru.jamsys.core.extension.builder;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings("unused")
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

    public HashSetBuilder<V> apply(Consumer<HashSetBuilder<V>> consumer) {
        consumer.accept(this);
        return this;
    }

}
