package ru.jamsys.extension;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ConsumerThrowing<T> {

    @SuppressWarnings("unused")
    void accept(T t) throws Exception;

}
