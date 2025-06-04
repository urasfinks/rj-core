package ru.jamsys.core.extension.expiration;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutable;
import ru.jamsys.core.extension.log.StatDataHeader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public abstract class AbstractExpirationElement
        extends AbstractLifeCycle
        implements ExpirationMsMutable,
        LifeCycleInterface,
        StatisticsFlush,
        CascadeKey {

    private long keepAliveOnInactivityMs = 6_000; // Время жизни если нет активности

    private volatile long lastActivityMs = System.currentTimeMillis();

    private Long stopTimeMs = null;

    @Override
    public List<StatDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

}
