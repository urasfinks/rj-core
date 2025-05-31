package ru.jamsys.core.pool;

import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;

public interface Pool<T extends AbstractExpirationResource> extends StatisticsFlush {

    // Взять элемент пула
    T acquire();

    // Вернуть элемент в пул
    void release(T ret, Throwable e);

    // Холостое добавление элемента в пустой пул (если min = 0), для обработки внешних потребителей
    boolean idleIfEmpty();

    // Вызывается когда в парк добавляется ресурс
    void onParkUpdate();

    // Если есть потребители, которые ждут ресурс - отдаём ресурс без вставки в park
    boolean forwardResourceWithoutParking(T poolItem);

}
