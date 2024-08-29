package ru.jamsys.core.promise;

import ru.jamsys.core.extension.RepositoryMap;

public interface RepositoryMapClass<V> extends RepositoryMap<String, V> {

    default <R> R setRepositoryMap(Class<R> cls, R obj) {
        return setRepositoryMap("main", cls, obj);
    }

    default <R> R setRepositoryMap(java.lang.String prefix, Class<R> cls, R obj) {
        return setRepositoryMap(prefix + "::" + cls.getName(), obj);
    }

    default <R> R getRepositoryMap(Class<R> cls) {
        return getRepositoryMap("main", cls);
    }

    default <R> R getRepositoryMap(java.lang.String prefix, Class<R> cls) {
        return getRepositoryMap(prefix + "::" + cls.getName(), cls, null);
    }

}
