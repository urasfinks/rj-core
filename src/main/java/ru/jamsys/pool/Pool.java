package ru.jamsys.pool;

public interface Pool<T> {

    @SuppressWarnings("unused")
    void complete(T ret, Exception e);

    @SuppressWarnings("unused")
    T getResource(Long timeOutMs) throws Exception;

    T createResource();

    void closeResource(T resource);

    @SuppressWarnings("unused")
    void keepAlive();

    String getName();

    boolean checkExceptionOnComplete(Exception e);

}
