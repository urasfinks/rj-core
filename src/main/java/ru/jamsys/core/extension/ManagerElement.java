package ru.jamsys.core.extension;

import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

public interface ManagerElement extends ExpirationMsMutable, StatisticsFlush, LifeCycleInterface {
    default void helper() {
    }
}
