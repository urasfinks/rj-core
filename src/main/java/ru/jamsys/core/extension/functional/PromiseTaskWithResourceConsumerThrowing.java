package ru.jamsys.core.extension.functional;

@FunctionalInterface
public interface PromiseTaskWithResourceConsumerThrowing<A, T, P, R> {

    /**
     * Performs the operation given the specified arguments.
     *
     * @param threadRun the first input argument
     * @param promiseTask the first input argument
     * @param promise     the second input argument
     * @param resource    the third input argument
     */

    void accept(A threadRun, T promiseTask, P promise, R resource) throws Throwable;
}
