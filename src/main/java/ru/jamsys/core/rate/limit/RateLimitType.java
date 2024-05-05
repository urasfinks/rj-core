package ru.jamsys.core.rate.limit;

import lombok.Getter;
import ru.jamsys.core.extension.EnumName;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;

@Getter
public enum RateLimitType implements EnumName {

    BROKER_SIZE(RateLimitItemInstance.MAX),
    BROKER_TAIL_SIZE(RateLimitItemInstance.MAX),
    BROKER_TPS(RateLimitItemInstance.TPS),
    POOL_SIZE(RateLimitItemInstance.MAX),
    THREAD_TPS(RateLimitItemInstance.TPS),
    POOL_ITEM_TPS(RateLimitItemInstance.TPS);

    final RateLimitItemInstance rateLimitItemInstance;

    final private String nameCache;

    RateLimitType(RateLimitItemInstance rateLimitItemInstance) {
        this.rateLimitItemInstance = rateLimitItemInstance;
        this.nameCache = getName();
    }

}
