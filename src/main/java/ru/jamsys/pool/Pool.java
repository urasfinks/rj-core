package ru.jamsys.pool;

public interface Pool<T> {

    T getResource() throws Exception;

    T createResource();

    void closeResource(T resource);

    PoolStatisticData flushStatistic();

    void stabilizer();

    String getName();

    void shutdown();

    boolean checkExceptionOnRemove(Exception e);

}
