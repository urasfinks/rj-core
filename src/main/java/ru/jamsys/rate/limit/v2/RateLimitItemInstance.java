package ru.jamsys.rate.limit.v2;

import ru.jamsys.extension.EnumName;
import ru.jamsys.template.cron.Unit;

public enum RateLimitItemInstance implements EnumName {
    TPS,
    TPM,
    TPH,
    TPD,
    TPW,
    MAX;

    RateLimitItem gen() {
        return switch (this) {
            case TPS -> new RateLimitItemTps();
            case TPM -> new RateLimitItemPeriodic(Unit.MINUTE);
            case TPH -> new RateLimitItemPeriodic(Unit.HOUR_OF_DAY);
            case TPD -> new RateLimitItemPeriodic(Unit.DAY_OF_MONTH);
            case TPW -> new RateLimitItemPeriodic(Unit.MONTH);
            case MAX -> new RateLimitItemMax();
        };
    }

}
