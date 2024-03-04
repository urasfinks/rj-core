package ru.jamsys.pool;

public interface Pool<T> {

    @SuppressWarnings("unused")
    void complete(T ret, Exception e);

    @SuppressWarnings("unused")
    T getResource(long timeOutMs) throws Exception;

    T createResource();

    void closeResource(T resource);

    @SuppressWarnings("unused")
    void stabilizer();

    String getName();

    @SuppressWarnings("unused")
    void run();

    @SuppressWarnings("unused")
    void shutdown();

    boolean checkExceptionOnComplete(Exception e);

}
