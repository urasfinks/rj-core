package ru.jamsys.core.extension.functional;

@FunctionalInterface
public interface TriConsumerThrowing<K, V, S> {

    /**
     * Performs the operation given the specified arguments.
     * @param k the first input argument
     * @param v the second input argument
     * @param s the third input argument
     */
    void accept(K k, V v, S s) throws Throwable;
}
