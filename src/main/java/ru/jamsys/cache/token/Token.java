package ru.jamsys.cache.token;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Token<V> {

    final long timestampAdd = System.currentTimeMillis();
    final long expired;
    final V value;

    public Token(V value, long timeoutMillis) {
        this.expired = timestampAdd + timeoutMillis;
        this.value = value;
    }
}
