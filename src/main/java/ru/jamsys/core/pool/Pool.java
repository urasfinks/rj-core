package ru.jamsys.core.pool;

import ru.jamsys.core.extension.StatisticsFlush;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Pool<T> extends StatisticsFlush {

    //После работы с ресурсом его надо вернуть в пул
    void complete(T ret, Exception e);

    // Получить ресурс без ожидания, если нет в park - вернём null
    @Deprecated
    T getPoolItem();

    //Если в parkQueue нет ресурса, будем ждать timeOutMs
    @Deprecated
    T getPoolItem(long timeOutMs, AtomicBoolean isThreadRun);

    // overclocking / onInitPool min resource / addPoolItemIfEmpty
    T createPoolItem();

    // Реализация закрытия ресурса
    void closePoolItem(T poolItem);

    String getName();

    //Реализация проверки ошибки, для принятия решений выкидывания ресурса из пула
    boolean checkExceptionOnComplete(Exception e);

    // Ручное удаление ресурса из пула, желательно конечно лишний раз не использовать
    void remove(T poolItem);

    // Ручное удаление ресурса из пула + вызов closePoolItem
    void removeAndClose(T poolItem);

    // Если min = 0, и в пуле никого нет, но есть внешний потребитель, которому нужны ресурсы в пуле
    // Добавляет ресурс в пустой пул
    void addIfPoolEmpty();

}
