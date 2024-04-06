package ru.jamsys.component.general.addable;

import java.util.Map;

public interface AddableMapItem<K, V> {

    Map<K, V> getMap();

    default void add(K key, V value) throws Exception {
        Map<K, V> map = getMap();
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }

}
