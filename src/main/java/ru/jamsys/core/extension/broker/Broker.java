package ru.jamsys.core.extension.broker;

import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

public interface Broker extends
        StatisticsFlush,
        ExpirationMsMutable,
        LifeCycleInterface,
        CascadeKey {

    long size(); // Размер данных

    boolean isEmpty(); // Пустой брокер

    @SuppressWarnings("unused")
    int getOccupancyPercentage(); // Процент заполненности очереди

    PropertyDispatcher<Integer> getPropertyDispatcher(); // Получить диспетчер настроек брокера

    // Рекомендуется использовать только для тестов
    void reset();

}
