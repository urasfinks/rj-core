package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceClassFinder;

import java.util.Map;

public interface Property<K, V> {

    Map<K, V> getMapProperty();

    default <R> R setProperty(K key, R obj) {
        @SuppressWarnings("unchecked")
        R result = (R) getMapProperty().computeIfAbsent(key, _ -> (V) obj);
        return result;
    }

    default Property<K, V> setProperty(Map<K, V> map) {
        getMapProperty().putAll(map);
        return this;
    }

    default <R> R getProperty(K key, Class<R> cls, R def) {
        Object o = getMapProperty().get(key);
        if (o != null && ServiceClassFinder.instanceOf(o.getClass(), cls)) {
            @SuppressWarnings("unchecked")
            R r = (R) o;
            return r;
        }
        return def;
    }

    default <R> R getProperty(K key, Class<R> cls) {
        return getProperty(key, cls, null);
    }

    default boolean isProperty(K key) {
        return getMapProperty().containsKey(key);
    }

}
