package ru.jamsys.core.extension;

import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.component.ServiceClassFinder;

import java.util.Map;
import java.util.function.Supplier;

public interface RepositoryMap<K, V> {

    Map<K, V> getRepositoryMap();

    // Я так полагаю, что если передаётся Supplier - то есть потребность лишний раз не создавать экземпляр value для map
    default <R> R setRepositoryMap(K key, Supplier<R> defSupplier) {
        V v = getRepositoryMap().computeIfAbsent(key, _ -> {
            R obj = defSupplier.get();
            @SuppressWarnings("unchecked")
            V r2v = (V) obj;
            return r2v;
        });
        @SuppressWarnings("unchecked")
        R v2r = (R) v;
        return v2r;
    }

    default <R> R setRepositoryMap(K key, R obj) {
        @SuppressWarnings("unchecked")
        V r2v = (V) obj;
        getRepositoryMap().put(key, r2v);
        return obj;
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
        return def;
    }

    default <R> R getRepositoryMap(@NotNull Class<R> cls, K key, Supplier<R> defSupplier) {
        Object o = getRepositoryMap().get(key);
        if (o != null && ServiceClassFinder.instanceOf(o.getClass(), cls)) {
            @SuppressWarnings("unchecked")
            R r = (R) o;
            return r;
        }
        return defSupplier.get();
    }

    default <R> R getRepositoryMap(@NotNull Class<R> cls, K key) {
        return getRepositoryMap(cls, key, (R) null);
    }

    default boolean repositoryMapContains(K key) {
        return getRepositoryMap().containsKey(key);
    }

}
