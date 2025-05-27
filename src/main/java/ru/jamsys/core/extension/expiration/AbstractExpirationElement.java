package ru.jamsys.core.extension.expiration;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutable;

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
}
