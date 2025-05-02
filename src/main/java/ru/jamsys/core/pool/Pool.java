package ru.jamsys.core.pool;

import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.resource.ResourceCheckException;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RT - ResourceArgument
// RR - ResourceResult
// PI - PoolItem

public interface Pool<T extends ExpirationMsMutable & Valid & ResourceCheckException> extends StatisticsFlush {

    //После работы с ресурсом его надо вернуть в пул
    void releasePoolItem(T ret, Throwable e);

    // overclocking / onInitPool min resource / addPoolItemIfEmpty
    T createPoolItem() ;

    // Реализация закрытия ресурса
    void closePoolItem(T poolItem);

    // Ручное удаление ресурса из пула, желательно конечно лишний раз не использовать
    void remove(T poolItem);

    // Ручное удаление ресурса из пула + вызов closePoolItem
    void removeAndClose(T poolItem);

    // Если min = 0, и в пуле никого нет, но есть внешний потребитель, которому нужны ресурсы в пуле.
    // Добавляет ресурс в пустой пул
    boolean isAvailablePoolItem();

    // Вызывается когда в парк добавляется ресурс
    void onParkUpdate();

    // Если есть потребители, которые ждут ресурс - отдаём ресурс без перевставок в park
    boolean forwardResourceWithoutParking(T poolItem);

    // Забрать ресурс из парка
    T get();

}
