package ru.jamsys.core.promise;


import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.extension.RepositoryMap;

public interface RepositoryMapClass<V> extends RepositoryMap<String, V> {

    default <R> R setRepositoryMapClass(@NotNull Class<R> cls, R obj) {
        return setRepositoryMapClass(cls, "main", obj);
    }

    default <R> R setRepositoryMapClass(@NotNull Class<R> cls, String prefix, R obj) {
        return setRepositoryMap(prefix + "::" + cls.getName(), obj);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls) {
        return getRepositoryMapClass(cls, "main");
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, R def) {
        return getRepositoryMapClass(cls, "main", def);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, String prefix) {
        return getRepositoryMap(cls, prefix + "::" + cls.getName(), null);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, String prefix, R def) {
        return getRepositoryMap(cls, prefix + "::" + cls.getName(), def);
    }

}
