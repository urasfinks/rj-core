package ru.jamsys;


@FunctionalInterface
public interface ConsumerThrowing<T> {

    void accept(T t) throws Exception;

}
