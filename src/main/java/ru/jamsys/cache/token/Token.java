package ru.jamsys.cache.token;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Token<V> {

    final long timeAddMs = System.currentTimeMillis();
    final long expired;
    final V value;

    public Token(V value, long timeoutMs) {
        this.expired = timeAddMs + timeoutMs;
        this.value = value;
    }
}
