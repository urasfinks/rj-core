package ru.jamsys.core.rate.limit;

import ru.jamsys.core.extension.EnumName;

public enum RateLimitName implements EnumName {
    POOL_SIZE_MAX,
    POOL_SIZE_MIN,
    BROKER_SIZE,
    BROKER_TAIL_SIZE,
    THREAD_TPS
}
