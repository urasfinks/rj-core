package ru.jamsys.core.extension.functional;

@SuppressWarnings("unused")
@FunctionalInterface
public interface SupplierThrowing<T> {

    @SuppressWarnings("unused")
    T get() throws Throwable;

}
