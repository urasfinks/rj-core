package ru.jamsys.core.rate.limit;

import ru.jamsys.core.extension.EnumName;

public enum RateLimitName implements EnumName {
    POOL_ITEM_TPS,
    POOL_SIZE,
    BROKER_SIZE,
    BROKER_TAIL_SIZE,
    BROKER_TPS,
    THREAD_TPS,
    FILE_LOG_SIZE,
    FILE_LOG_INDEX
}
