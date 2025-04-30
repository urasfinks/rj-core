package ru.jamsys.core.rate.limit;

import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemPeriodic;
import ru.jamsys.core.rate.limit.item.RateLimitItemTps;

@FieldNameConstants
public enum RateLimitFactory implements CamelNormalization {

    TPS, // В секунду
    TPMin, // В минуту
    TPH, // В час
    TPD, // В день
    TPMo; //  В месяц

    public RateLimitItem create(String ns) {
        return switch (this) {
            case TPS -> new RateLimitItemTps(ns);
            case TPMin -> new RateLimitItemPeriodic(TimeUnit.MINUTE, ns);
            case TPH -> new RateLimitItemPeriodic(TimeUnit.HOUR_OF_DAY, ns);
            case TPD -> new RateLimitItemPeriodic(TimeUnit.DAY_OF_MONTH, ns);
            case TPMo -> new RateLimitItemPeriodic(TimeUnit.MONTH, ns);
        };
    }

}
