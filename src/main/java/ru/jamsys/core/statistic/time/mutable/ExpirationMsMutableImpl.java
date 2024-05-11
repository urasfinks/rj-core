package ru.jamsys.core.statistic.time.mutable;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExpirationMsMutableImpl implements ExpirationMsMutable {

    private long keepAliveOnInactivityMs = 6_000; // Время жизни если нет активности

    private volatile long lastActivityMs = System.currentTimeMillis();

    private Long stopTimeMs = null;

}
