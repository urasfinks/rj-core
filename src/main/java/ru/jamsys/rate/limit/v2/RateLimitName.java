package ru.jamsys.rate.limit.v2;

import ru.jamsys.extension.EnumName;

public enum RateLimitName implements EnumName {
    TPS,
    MAX,
    BROKER_SIZE,
    BROKER_TPS,
    POOL_SIZE,
    THREAD_TPS,
    RESOURCE_TPS
}
