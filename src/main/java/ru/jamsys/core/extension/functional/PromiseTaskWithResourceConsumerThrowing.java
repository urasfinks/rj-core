package ru.jamsys.core.extension.functional;

@FunctionalInterface
public interface PromiseTaskWithResourceConsumerThrowing<T, A, P, R> {

    /**
     * Performs the operation given the specified arguments.
     *
     * @param isThreadRun the first input argument
     * @param promiseTask the first input argument
     * @param promise     the second input argument
     * @param resource    the third input argument
     */

    void accept(A isThreadRun, T promiseTask, P promise, R resource) throws Throwable;
}
