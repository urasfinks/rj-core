package ru.jamsys.rate.limit;

import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.rate.limit.item.RateLimitItemInstance;

public interface RateLimit extends StatisticsCollector {

    boolean isActive();

    void setActive(boolean active);

    @SuppressWarnings("unused")
    String getMomentumStatistic();

    void reset();

    // Необходимо соблюдать идентичность классификации вызываемых проверок
    // Нельзя использовать MAX и TPS одновременно - всегда будет false
    boolean check(Integer limit);

    RateLimitItem get(String name, RateLimitItemInstance rateLimitItemInstance);

    default RateLimitItem get(RateLimitName name) {
        return get(name.getNameCache(), name.getRateLimitItemInstance());
    }

}