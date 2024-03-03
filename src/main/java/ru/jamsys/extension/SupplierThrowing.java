package ru.jamsys.extension;

@SuppressWarnings("unused")
@FunctionalInterface
public interface SupplierThrowing<T> {

    @SuppressWarnings("unused")
    T get() throws Exception;

}
