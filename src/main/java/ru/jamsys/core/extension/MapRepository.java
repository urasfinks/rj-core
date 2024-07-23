package ru.jamsys.core.extension;

import ru.jamsys.core.component.ServiceClassFinder;

import java.util.Map;

public interface MapRepository<K, V> {

    Map<K, V> getMapRepository();

    default <R> R setToMapRepository(K key, R obj) {
        @SuppressWarnings("unchecked")
        R result = (R) getMapRepository().computeIfAbsent(key, _ -> (V) obj);
        return result;
    }

    default MapRepository<K, V> setToMapRepository(Map<K, V> map) {
        getMapRepository().putAll(map);
        return this;
    }

    default <R> R getFromMapRepository(K key, Class<R> cls, R def) {
        Object o = getMapRepository().get(key);
        if (o != null && ServiceClassFinder.instanceOf(o.getClass(), cls)) {
            @SuppressWarnings("unchecked")
            R r = (R) o;
            return r;
        }
        return def;
    }

    default <R> R getFromMapRepository(K key, Class<R> cls) {
        return getFromMapRepository(key, cls, null);
    }

    default boolean mapRepositoryContains(K key) {
        return getMapRepository().containsKey(key);
    }

}
