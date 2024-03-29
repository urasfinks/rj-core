package ru.jamsys.rate.limit;

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

    RateLimitItem get(String name, RateLimitItemInstance rateLimitItemInstance);

    default RateLimitItem get(RateLimitName name) {
        return get(name.getName(), name.getRateLimitItemInstance());
    }

}
