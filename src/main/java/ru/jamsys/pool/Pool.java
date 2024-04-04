package ru.jamsys.pool;

import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.TimeController;
import ru.jamsys.thread.ThreadEnvelope;

public interface Pool<T extends TimeController> extends StatisticsCollector {

    @SuppressWarnings("unused")
    void complete(T ret, Exception e);

    @SuppressWarnings("unused")
    T getResource(); // Получить ресурс без ожидания, если нет в park - вернём null

    @SuppressWarnings("unused")
    T getResource(long timeOutMs, ThreadEnvelope threadEnvelope); //Если в parkQueue нет ресурса, будем ждать timeOutMs

    T createResource();

    void closeResource(T resource);

    @SuppressWarnings("unused")
    void keepAlive();

    String getName();

    boolean checkExceptionOnComplete(Exception e);

    void remove(T resource);

    void removeAndClose(T resource);

    void addResourceZeroPool();

    void setSumTime(long time);

}
