package ru.jamsys.cache;

import ru.jamsys.cache.token.Token;
import ru.jamsys.util.Util;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenManager<K, V> {
    final Map<K, Token<V>> map = new ConcurrentHashMap<>();

    public boolean add(K key, V value, long timeoutMs) {
        flush();
        if (!map.containsKey(key)) {
            map.put(key, new Token<>(value, timeoutMs));
            return true;
        }
        return false;
    }

    public V get(K key) {
        if (map.containsKey(key)) {
            return map.get(key).getValue();
        }
        return null;
    }

    public Map<K, Token<V>> get() {
        return map;
    }

    @SafeVarargs
    static <K> K[] getEmptyType(K... array) {
        return Arrays.copyOf(array, 0);
    }

    private void flush() {
        long curMs = System.currentTimeMillis();
        Util.riskModifierMap(null, map, getEmptyType(), (K key, Token<V> value) -> {
            if (curMs > value.getExpired()) {
                map.remove(key);
            }
        });
    }
}
