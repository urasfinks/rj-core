package ru.jamsys.cache;

import lombok.Data;

@Data
public class Token<V> {

    final long timestampAdd = System.currentTimeMillis();
    final long expired;
    final V value;

    public Token(V value, long timeoutMillis) {
        this.expired = timestampAdd + timeoutMillis;
        this.value = value;
    }
}
