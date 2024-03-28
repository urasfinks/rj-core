package ru.jamsys.rate.limit.v2;

import java.util.Map;

public interface RateLimit {

    boolean isActive();

    void setActive(boolean active);

    Map<String, Object> flushTps(long curTime);

    @SuppressWarnings("unused")
    String getMomentumStatistic();

    void reset();

    // Необходимо соблюдать идентичность классификации вызываемых проверок
    // Нельзя использовать MAX и TPS одновременно - всегда будет false
    boolean check(Integer limit);

    RateLimitItem add(String name, RateLimitItemInstance rateLimitItemInstance);

    RateLimitItem add(RateLimitName name, RateLimitItemInstance rateLimitItemInstance);

    RateLimitItem get(String name);

    RateLimitItem get(RateLimitName name);

}
