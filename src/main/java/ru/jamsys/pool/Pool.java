package ru.jamsys.pool;

public interface Pool<T> {

    @SuppressWarnings("unused")
    void complete(T ret, Exception e);

    @SuppressWarnings("unused")
    T getResource() throws Exception;

    T createResource();

    void closeResource(T resource);

    @SuppressWarnings("unused")
    PoolStatisticData flushStatistic();

    @SuppressWarnings("unused")
    void stabilizer();

    String getName();

    @SuppressWarnings("unused")
    void shutdown();

    boolean checkExceptionOnRemove(Exception e);

}
