package ru.jamsys.core.rate.limit;

import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemPeriodic;
import ru.jamsys.core.rate.limit.item.RateLimitItemTps;

public enum RateLimitFactory implements CamelNormalization {

    TPS, // В секунду
    TPMin, // В минуту
    TPH, // В час
    TPD, // В день
    TPMo; //  В месяц

    public RateLimitItem create(String namespace) {
        return switch (this) {
            case TPS -> new RateLimitItemTps(namespace);
            case TPMin -> new RateLimitItemPeriodic(TimeUnit.MINUTE, namespace);
            case TPH -> new RateLimitItemPeriodic(TimeUnit.HOUR_OF_DAY, namespace);
            case TPD -> new RateLimitItemPeriodic(TimeUnit.DAY_OF_MONTH, namespace);
            case TPMo -> new RateLimitItemPeriodic(TimeUnit.MONTH, namespace);
        };
    }

}
