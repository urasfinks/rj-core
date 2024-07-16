package ru.jamsys.core.extension.functional.iface;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ConsumerThrowing<T> {

    @SuppressWarnings("unused")
    void accept(T t) throws Exception;

}
