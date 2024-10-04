package ru.jamsys.core.extension.functional;

@SuppressWarnings("unused")
@FunctionalInterface
public interface TriFunctionThrowing<T, U, V, R> {

    @SuppressWarnings("unused")
    R apply(T t, U u, V v) throws Throwable;

}
