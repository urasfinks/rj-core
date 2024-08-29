package ru.jamsys.core.extension;

import ru.jamsys.core.component.ServiceClassFinder;

import java.util.Map;

public interface RepositoryMap<K, V> {

    Map<K, V> getRepositoryMap();

    // Буд-те внимательны хранилище нельзя перезаписывать по ключу!!!
    default <R> R setRepositoryMap(K key, R obj) {
        @SuppressWarnings("unchecked")
        R result = (R) getRepositoryMap().computeIfAbsent(key, _ -> (V) obj);
        return result;
    }

    default RepositoryMap<K, V> setRepositoryMap(Map<K, V> map) {
        getRepositoryMap().putAll(map);
        return this;
    }

    default <R> R getRepositoryMap(K key, Class<R> cls, R def) {
        Object o = getRepositoryMap().get(key);
        if (o != null && ServiceClassFinder.instanceOf(o.getClass(), cls)) {
            @SuppressWarnings("unchecked")
            R r = (R) o;
            return r;
        }
        return def;
    }

    default <R> R getRepositoryMap(K key, Class<R> cls) {
        return getRepositoryMap(key, cls, null);
    }

    default boolean repositoryMapContains(K key) {
        return getRepositoryMap().containsKey(key);
    }

}
