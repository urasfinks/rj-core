package ru.jamsys.core.extension.functional;

@FunctionalInterface
public interface PromiseTaskConsumerThrowing<T, A, P> {

    /**
     * Весь этот сыр бор из-за структурирования ошибок и принадлежности их к задачам
     * Раньше передавали только Promise и из выполняемого кода нельзя определить, что это за задача исполняется
     * Для уменьшения аргументов, можно было избавиться от Promise вообще, внутри блока получать Promise через
     * promiseTask.getPromise(), но в большенстве случаев мы работаем с Promise, а не promiseTask
     * поэтому я решил оставить оба аргумента, что бы в каждой задаче не дублировался код:
     * Promise promise = promiseTask.getPromise();
     * Приятно было бы через Promise получать задачу на подобии: promise.getTask(), но тут встаёт проблема параллельного
     * исполнения задач, это значит, что статичный ключ исполняемой задачи сделать нельзя, а это значит что надо будет
     * создавать обёртки для Promise на подобии PromiseDebug и хранить в каждой ссылку на задачу
     * что-то я не хочу обёрток, не хочу кучи ссылок - пусть будут просто аргументы promiseTask и promise
     * ----
     * Performs the operation given the specified arguments.
     * @param isThreadRun the first input argument
     * @param promiseTask the first input argument
     * @param promise the second input argument
     */
    void accept(A isThreadRun, T promiseTask, P promise) throws Throwable;
}
