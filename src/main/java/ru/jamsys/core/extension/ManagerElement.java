package ru.jamsys.core.extension;

import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutable;

public interface ManagerElement extends ExpirationMsMutable, StatisticsFlush, LifeCycleInterface {

    default void helper() {
    }

}
