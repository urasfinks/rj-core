package ru.jamsys.core.extension.expiration.mutable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Setter
@Getter
@ToString
public class ExpirationMsMutableImpl implements ExpirationMsMutable, Serializable {

    private long inactivityTimeoutMs = 6_000; // Время жизни если нет активности

    private volatile long lastActivityMs = System.currentTimeMillis();

    private Long stopTimeMs = null;

}
