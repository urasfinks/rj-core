package ru.jamsys.core.rate.limit;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.EnumName;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemPeriodic;
import ru.jamsys.core.rate.limit.item.RateLimitItemTps;

public enum RateLimitFactory implements EnumName {

    TPS,
    TPM,
    TPH,
    TPD,
    TPW;

    public RateLimitItem create(ApplicationContext applicationContext, String ns) {
        return switch (this) {
            case TPS -> new RateLimitItemTps(applicationContext, ns);
            case TPM -> new RateLimitItemPeriodic(applicationContext, TimeUnit.MINUTE, ns);
            case TPH -> new RateLimitItemPeriodic(applicationContext, TimeUnit.HOUR_OF_DAY, ns);
            case TPD -> new RateLimitItemPeriodic(applicationContext, TimeUnit.DAY_OF_MONTH, ns);
            case TPW -> new RateLimitItemPeriodic(applicationContext, TimeUnit.MONTH, ns);
        };
    }

}
