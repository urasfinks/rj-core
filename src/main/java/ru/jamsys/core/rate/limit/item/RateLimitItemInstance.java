package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.extension.EnumName;
import ru.jamsys.core.flat.template.cron.TimeUnit;

public enum RateLimitItemInstance implements EnumName {

    MIN,
    TPS,
    TPM,
    TPH,
    TPD,
    TPW,
    MAX;

    public RateLimitItem create(String ns) {
        return switch (this) {
            case MIN -> new RateLimitItemMin(ns);
            case TPS -> new RateLimitItemTps(ns);
            case TPM -> new RateLimitItemPeriodic(TimeUnit.MINUTE, ns);
            case TPH -> new RateLimitItemPeriodic(TimeUnit.HOUR_OF_DAY, ns);
            case TPD -> new RateLimitItemPeriodic(TimeUnit.DAY_OF_MONTH, ns);
            case TPW -> new RateLimitItemPeriodic(TimeUnit.MONTH, ns);
            case MAX -> new RateLimitItemMax(ns);
        };
    }

}
