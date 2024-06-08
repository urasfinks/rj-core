package ru.jamsys.core.rate.limit.item;

import org.springframework.context.ApplicationContext;
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

    public RateLimitItem create(ApplicationContext applicationContext, String ns) {
        return switch (this) {
            case MIN -> new RateLimitItemMin(applicationContext, ns);
            case TPS -> new RateLimitItemTps(applicationContext, ns);
            case TPM -> new RateLimitItemPeriodic(applicationContext, TimeUnit.MINUTE, ns);
            case TPH -> new RateLimitItemPeriodic(applicationContext, TimeUnit.HOUR_OF_DAY, ns);
            case TPD -> new RateLimitItemPeriodic(applicationContext, TimeUnit.DAY_OF_MONTH, ns);
            case TPW -> new RateLimitItemPeriodic(applicationContext, TimeUnit.MONTH, ns);
            case MAX -> new RateLimitItemMax(applicationContext, ns);
        };
    }

}
