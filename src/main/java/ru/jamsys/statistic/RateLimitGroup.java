package ru.jamsys.statistic;

import ru.jamsys.extension.EnumName;

/*
 * Появились пересечения лимитов в одном объекте ThreadPool, когда есть лимит пула и есть лимит по операциям в Thread
 * RateLimitGroup позволяет разграничить ключи в RateLimit
 * */

public enum RateLimitGroup implements EnumName {
    @SuppressWarnings("unused") OTHER,
    THREAD,
    RESOURCE,
    BROKER,
    POOL
}
