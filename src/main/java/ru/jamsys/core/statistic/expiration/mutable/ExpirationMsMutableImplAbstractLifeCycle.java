package ru.jamsys.core.statistic.expiration.mutable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.AbstractLifeCycle;

import java.io.Serializable;

// TODO: попробовать убрать Serializable

@Setter
@Getter
@ToString
public abstract class ExpirationMsMutableImplAbstractLifeCycle
        extends AbstractLifeCycle
        implements ExpirationMsMutable, Serializable {

    private long keepAliveOnInactivityMs = 6_000; // Время жизни если нет активности

    private volatile long lastActivityMs = System.currentTimeMillis();

    private Long stopTimeMs = null;

}
