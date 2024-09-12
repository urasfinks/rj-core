package ru.jamsys.core.promise;


import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.extension.RepositoryMap;

import java.util.function.Supplier;

public interface RepositoryMapClass<V> extends RepositoryMap<String, V> {

    default <R> R setRepositoryMapClass(@NotNull Class<R> cls, R obj) {
        return setRepositoryMapClass(cls, "main", obj);
    }

    default <R> R setRepositoryMapClass(@NotNull Class<R> cls, String prefix, R obj) {
        return setRepositoryMap(prefix + "::" + cls.getName(), obj);
    }

    default <R> R setRepositoryMapClass(@NotNull Class<R> cls, String prefix, Supplier<R> defSupplier) {
        return setRepositoryMap(prefix + "::" + cls.getName(), defSupplier);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls) {
        return getRepositoryMapClass(cls, "main");
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, R def) {
        return getRepositoryMapClass(cls, "main", def);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, Supplier<R> defSupplier) {
        return getRepositoryMapClass(cls, "main", defSupplier);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, String prefix) {
        return getRepositoryMap(cls, prefix + "::" + cls.getName(), (R) null);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, String prefix, R def) {
        return getRepositoryMap(cls, prefix + "::" + cls.getName(), def);
    }

    default <R> R getRepositoryMapClass(@NotNull Class<R> cls, String prefix, Supplier<R> defSupplier) {
        return getRepositoryMap(cls, prefix + "::" + cls.getName(), defSupplier);
    }

}
