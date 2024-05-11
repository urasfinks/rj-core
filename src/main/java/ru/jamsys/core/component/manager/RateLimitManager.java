package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;

/*
 * RateLimitItem целевого механизма по удалению элементов - нет
 * Так как если в runTime будут выставлены для кого-то лимиты, а потом этот объект
 * Временно остановится и вместе с собой удалит установленные лимиты, то после восстановления работы
 * ранее установленные лимиты будут утрачены? Это как-то странно?
 * Можно только управлять статусом active = true/false для отрисовки статистики
 * */

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class RateLimitManager
        extends AbstractManagerMapItem<RateLimit, RateLimitItem>
        implements StatisticsFlushComponent {

    public RateLimitManager() {
        setCleanableMap(false);
    }

    @Override
    public RateLimit build(String index) {
        return new RateLimit(index);
    }

}
