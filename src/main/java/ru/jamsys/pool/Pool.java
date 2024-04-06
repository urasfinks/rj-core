package ru.jamsys.pool;

import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.thread.ThreadEnvelope;

public interface Pool<T> extends StatisticsCollector {

    @SuppressWarnings("unused")
    void complete(T ret, Exception e);

    @SuppressWarnings("unused")
    T getPoolItem(); // Получить ресурс без ожидания, если нет в park - вернём null

    @SuppressWarnings("unused")
    T getPoolItem(long timeOutMs, ThreadEnvelope threadEnvelope); //Если в parkQueue нет ресурса, будем ждать timeOutMs

    T createPoolItem();

    void closePoolItem(T poolItem);

    String getName();

    boolean checkExceptionOnComplete(Exception e);

    void remove(T poolItem);

    void removeAndClose(T poolItem);

    void addPoolItemIfEmpty();

    void setSumTime(long time);

}
