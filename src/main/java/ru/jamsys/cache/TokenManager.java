package ru.jamsys.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenManager<K, V> {
    final Map<K, Token<V>> map = new ConcurrentHashMap<>();

    public boolean add(K key, V value, long timeoutMillis) {
        flush();
        if (!map.containsKey(key)) {
            map.put(key, new Token<>(value, timeoutMillis));
            return true;
        }
        return false;
    }

    public V get(K key) {
        return map.get(key).getValue();
    }

    public Map<K, Token<V>> get() {
        return map;
    }

    private void flush() {
        Object[] keys = map.keySet().toArray();
        long curTimestamp = System.currentTimeMillis();
        for (Object key : keys) {
            Token<V> codeObject = map.get(key);
            if (codeObject != null && curTimestamp > codeObject.getExpired()) {
                map.remove(key);
            }
        }
    }
}
