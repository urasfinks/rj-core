package ru.jamsys.core.extension.functional;

@SuppressWarnings("unused")
@FunctionalInterface
public interface FunctionThrowing<T, R > {

    @SuppressWarnings("unused")
    R apply(T t) throws Throwable;

}
