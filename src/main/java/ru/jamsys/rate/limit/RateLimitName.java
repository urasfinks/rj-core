package ru.jamsys.rate.limit;

import lombok.Getter;
import ru.jamsys.extension.EnumName;
import ru.jamsys.rate.limit.item.RateLimitItemInstance;

public enum RateLimitName implements EnumName {

    BROKER_SIZE(RateLimitItemInstance.MAX),
    BROKER_TPS(RateLimitItemInstance.TPS),
    POOL_SIZE(RateLimitItemInstance.MAX),
    THREAD_TPS(RateLimitItemInstance.TPS),
    RESOURCE_TPS(RateLimitItemInstance.TPS);

    @Getter
    final RateLimitItemInstance rateLimitItemInstance;

    RateLimitName(RateLimitItemInstance rateLimitItemInstance) {
        this.rateLimitItemInstance = rateLimitItemInstance;
    }

}
