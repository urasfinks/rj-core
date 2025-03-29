package ru.jamsys.core.extension.broker.persist;

import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

public interface Broker<T> extends
        StatisticsFlush,
        ExpirationMsMutable,
        ClassEquals,
        LifeCycleInterface,
        KeepAlive, AddToList<
        ExpirationMsImmutableEnvelope<T>,
        DisposableExpirationMsImmutableEnvelope<T> // Должны вернуть, что бы из вне можно было сделать remove
        > {

    int size(); // Размер данных

    boolean isEmpty(); // Пустой брокер

    int getOccupancyPercentage(); // Процент заполненности очереди

    // Добавление с явным указанием времени
    DisposableExpirationMsImmutableEnvelope<T> add(T element, long curTime, long timeOut);

    // Добавление без явного указания времени
    DisposableExpirationMsImmutableEnvelope<T> add(T element, long timeOutMs);

    PropertyDispatcher<Integer> getPropertyDispatcher(); // Получить диспетчер настроек брокера

    //BrokerProperty getPropertyBroke(); // Gjkexb

    // Рекомендуется использовать только для тестов
    void reset();

}
