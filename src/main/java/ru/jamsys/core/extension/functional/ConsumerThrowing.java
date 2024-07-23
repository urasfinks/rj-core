package ru.jamsys.core.extension.functional;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ConsumerThrowing<T> {

    @SuppressWarnings("unused")
    void accept(T t) throws Exception;

}
