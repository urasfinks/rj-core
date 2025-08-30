package ru.jamsys.core.extension;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.expiration.Expiration;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutable;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public abstract class AbstractManagerElement extends AbstractLifeCycle
        implements ExpirationMsMutable,
        LifeCycleInterface,
        StatisticsFlush,
        Expiration,
        CascadeKey {

    private long inactivityTimeoutMs = 6_000; // Время жизни если нет активности

    private volatile long lastActivityMs = System.currentTimeMillis();

    private Long stopTimeMs = null;

    @Override
    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

    public void helper() {
    }

    @Override
    public void onExpired() {

    }

}
