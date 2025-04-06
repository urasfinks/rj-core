package ru.jamsys.core.pool;

import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.extension.StatisticsFlush;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

public interface Pool<RA, RR, PI extends Resource<RA, RR>> extends StatisticsFlush {

    //После работы с ресурсом его надо вернуть в пул
    void completePoolItem(PI ret, Throwable e);

    // overclocking / onInitPool min resource / addPoolItemIfEmpty
    PI createPoolItem() ;

    // Реализация закрытия ресурса
    void closePoolItem(PI poolItem);

    //Реализация проверки ошибки, для принятия решений выкидывания ресурса из пула
    boolean checkFatalException(Throwable th);

    // Ручное удаление ресурса из пула, желательно конечно лишний раз не использовать
    void remove(PI poolItem);

    // Ручное удаление ресурса из пула + вызов closePoolItem
    void removeAndClose(PI poolItem);

    // Если min = 0, и в пуле никого нет, но есть внешний потребитель, которому нужны ресурсы в пуле.
    // Добавляет ресурс в пустой пул
    boolean isAvailablePoolItem();

    // Вызывается когда в парк добавляется ресурс
    void onParkUpdate();

    // Если есть потребители, которые ждут ресурс - отдаём ресурс без перевставок в park
    boolean forwardResourceWithoutParking(PI poolItem);

    // Забрать взят ресурс из парка
    PI getFromPark();

}
