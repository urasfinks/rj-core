package ru.jamsys.core.extension.functional;

@FunctionalInterface
public interface BiConsumerThrowing<T, U> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    void accept(T t, U u) throws Throwable;
}
