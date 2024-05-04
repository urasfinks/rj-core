package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.extension.EnumName;
import ru.jamsys.core.template.cron.TimeUnit;

public enum RateLimitItemInstance implements EnumName {

    TPS,
    TPM,
    TPH,
    TPD,
    TPW,
    MAX;

    public RateLimitItem create() {
        return switch (this) {
            case TPS -> new RateLimitItemTps();
            case TPM -> new RateLimitItemPeriodic(TimeUnit.MINUTE);
            case TPH -> new RateLimitItemPeriodic(TimeUnit.HOUR_OF_DAY);
            case TPD -> new RateLimitItemPeriodic(TimeUnit.DAY_OF_MONTH);
            case TPW -> new RateLimitItemPeriodic(TimeUnit.MONTH);
            case MAX -> new RateLimitItemMax();
        };
    }

}
