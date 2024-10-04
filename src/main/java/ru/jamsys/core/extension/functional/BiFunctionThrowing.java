package ru.jamsys.core.extension.functional;

@SuppressWarnings("unused")
@FunctionalInterface
public interface BiFunctionThrowing<T, U, R> {

    @SuppressWarnings("unused")
    R apply(T t, U u) throws Throwable;

}
