package ru.jamsys.core.extension;

import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.component.ServiceClassFinder;

import java.util.Map;
import java.util.function.Supplier;

public interface RepositoryMap<K, V> {

    Map<K, V> getRepositoryMap();

    // Буд-те внимательны хранилище нельзя перезаписывать по ключу!!!
    default <R> R setRepositoryMap(K key, Supplier<R> defSupplier) {
        @SuppressWarnings("unchecked")
        R result = (R) getRepositoryMap().computeIfAbsent(key, _ -> (V) defSupplier.get());
        return result;
    }

    default <R> R setRepositoryMap(K key, R obj) {
        @SuppressWarnings("unchecked")
        R result = (R) getRepositoryMap().computeIfAbsent(key, _ -> (V) obj);
        return result;
    }

    default RepositoryMap<K, V> setRepositoryMap(Map<K, V> map) {
        getRepositoryMap().putAll(map);
        return this;
    }

    default <R> R getRepositoryMap(@NotNull Class<R> cls, K key, R def) {
        Object o = getRepositoryMap().get(key);
        if (o != null && ServiceClassFinder.instanceOf(o.getClass(), cls)) {
            @SuppressWarnings("unchecked")
            R r = (R) o;
            return r;
        }
        return setRepositoryMap(key, def);
    }

    default <R> R getRepositoryMap(@NotNull Class<R> cls, K key, Supplier<R> defSupplier) {
        Object o = getRepositoryMap().get(key);
        if (o != null && ServiceClassFinder.instanceOf(o.getClass(), cls)) {
            @SuppressWarnings("unchecked")
            R r = (R) o;
            return r;
        }
        return setRepositoryMap(key, defSupplier.get());
    }

    default <R> R getRepositoryMap(@NotNull Class<R> cls, K key) {
        return getRepositoryMap(cls, key, (R) null);
    }

    default boolean repositoryMapContains(K key) {
        return getRepositoryMap().containsKey(key);
    }

}
