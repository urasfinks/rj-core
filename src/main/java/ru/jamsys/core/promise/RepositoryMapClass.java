package ru.jamsys.core.promise;

import ru.jamsys.core.extension.RepositoryMap;

public interface RepositoryMapClass<V> extends RepositoryMap<String, V> {

    default <R> R setRepositoryMapClass(Class<R> cls, R obj) {
        return setRepositoryMapClass("main", cls, obj);
    }

    default <R> R setRepositoryMapClass(java.lang.String prefix, Class<R> cls, R obj) {
        return setRepositoryMap(prefix + "::" + cls.getName(), obj);
    }

    default <R> R getRepositoryMapClass(Class<R> cls) {
        return getRepositoryMapClass("main", cls);
    }

    default <R> R getRepositoryMapClass(java.lang.String prefix, Class<R> cls) {
        return getRepositoryMap(prefix + "::" + cls.getName(), cls, null);
    }

}
