package ru.jamsys;

@FunctionalInterface
public interface SupplierThrowing<T> {
    T get() throws Exception;
}
