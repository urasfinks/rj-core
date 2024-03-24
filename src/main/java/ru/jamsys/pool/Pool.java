package ru.jamsys.pool;

import ru.jamsys.statistic.Expired;

public interface Pool<T extends Expired> {

    @SuppressWarnings("unused")
    default void complete(T ret, Exception e) {
        complete(ret, e, true);
    }

    @SuppressWarnings("unused")
    void complete(T ret, Exception e, boolean isFinish);

    @SuppressWarnings("unused")
    T getResource(); // Получить ресурс без ожидания, если нет в park - вернём null

    @SuppressWarnings("unused")
    T getResource(Long timeOutMs); //Если в parkQueue нет ресурса, будем ждать timeOutMs

    T createResource();

    void closeResource(T resource);

    @SuppressWarnings("unused")
    void keepAlive();

    String getName();

    boolean checkExceptionOnComplete(Exception e);

    void removeForce(T resource, boolean isFinish);

    void addResourceZeroPool();

}
