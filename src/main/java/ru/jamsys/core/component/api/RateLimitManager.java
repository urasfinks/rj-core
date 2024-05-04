package ru.jamsys.core.component.api;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractComponentMap;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitType;
import ru.jamsys.core.rate.limit.item.RateLimitItem;

/*
 * RateLimitItem целевого механизма по удалению элементов - нет
 * Так как если в runTime будут выставлены для кого-то лимиты, а потом этот объект
 * Временно остановится и вместе с собой удалит установленные лимиты, то после восстановления работы
 * ранее установленные лимиты будут утрачены? Это как-то странно?
 * Можно только управлять статусов active = true/false для отрисовки статистики
 * */

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class RateLimitManager
        extends AbstractComponentMap<RateLimit, RateLimitItem>
        implements StatisticsFlushComponent {

    public RateLimitManager() {
        setCleanableMap(false);
    }

    @Override
    public RateLimit build(String key) {
        return new RateLimit();
    }

    public String getKey(@NonNull Class<?> clsOwner, String key) {
        if (key == null) {
            return clsOwner.getSimpleName();
        } else {
            return clsOwner.getSimpleName() + "." + key;
        }
    }

    public RateLimit get(@NonNull Class<?> clsOwner, @Nullable String key) {
        return getItem(getKey(clsOwner, key));
    }

    public boolean contains(@NonNull Class<?> clsOwner, @Nullable String key) {
        return containsKey(getKey(clsOwner, key));
    }

    public void initLimit(Class<?> clsOwner, String key, RateLimitType rateLimitType, int max) {
        get(clsOwner, key).get(rateLimitType).setMax(max);
    }

}
