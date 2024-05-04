package ru.jamsys.core.extension.addable;

import java.util.Map;

@SuppressWarnings("unused")
public interface AddableMapItem<K, V> {

    Map<K, V> getMap();

    default void add(K key, V value) {
        Map<K, V> map = getMap();
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }

}
