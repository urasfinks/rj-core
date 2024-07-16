package ru.jamsys.core.extension.functional.iface;

@SuppressWarnings("unused")
@FunctionalInterface
public interface SupplierThrowing<T> {

    @SuppressWarnings("unused")
    T get() throws Exception;

}
