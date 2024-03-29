package ru.jamsys.rate.limit.item;

import ru.jamsys.extension.EnumName;
import ru.jamsys.template.cron.Unit;

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
            case TPM -> new RateLimitItemPeriodic(Unit.MINUTE);
            case TPH -> new RateLimitItemPeriodic(Unit.HOUR_OF_DAY);
            case TPD -> new RateLimitItemPeriodic(Unit.DAY_OF_MONTH);
            case TPW -> new RateLimitItemPeriodic(Unit.MONTH);
            case MAX -> new RateLimitItemMax();
        };
    }

}
