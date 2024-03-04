package ru.jamsys.cache.token;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Token<V> {

    final long timeAddMs = System.currentTimeMillis();
    final long expiredMs;
    final V value;

    public Token(V value, long timeoutMs) {
        this.expiredMs = timeAddMs + timeoutMs;
        this.value = value;
    }
}
