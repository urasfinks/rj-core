package ru.jamsys.statistic;

import ru.jamsys.util.Util;

/*
 * Появились пересечения лимитов в одном объекте ThreadPool, когда есть лимит пула и есть лимит по операциям в Thread
 * RateLimitGroup позволяет разграничить ключи в RateLimit
 * */

public enum RateLimitGroup {
    @SuppressWarnings("unused") OTHER,
    THREAD,
    RESOURCE,
    BROKER,
    POOL;

    public String getName() {
        return Util.snakeToCamel(name());
    }

}
