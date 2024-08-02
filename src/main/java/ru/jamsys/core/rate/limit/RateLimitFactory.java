package ru.jamsys.core.rate.limit;

import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemPeriodic;
import ru.jamsys.core.rate.limit.item.RateLimitItemTps;

public enum RateLimitFactory implements CamelNormalization {

    TPS,
    TPM,
    TPH,
    TPD,
    TPW;

    public RateLimitItem create(String ns) {
        return switch (this) {
            case TPS -> new RateLimitItemTps(ns);
            case TPM -> new RateLimitItemPeriodic(TimeUnit.MINUTE, ns);
            case TPH -> new RateLimitItemPeriodic(TimeUnit.HOUR_OF_DAY, ns);
            case TPD -> new RateLimitItemPeriodic(TimeUnit.DAY_OF_MONTH, ns);
            case TPW -> new RateLimitItemPeriodic(TimeUnit.MONTH, ns);
        };
    }

}
