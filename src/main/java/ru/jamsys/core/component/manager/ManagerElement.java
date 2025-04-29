package ru.jamsys.core.component.manager;

import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

public interface ManagerElement extends ExpirationMsMutable, StatisticsFlush, LifeCycleInterface {
}
