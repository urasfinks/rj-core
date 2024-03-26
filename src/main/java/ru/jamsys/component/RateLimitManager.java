package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.rate.limit.RateLimit;
import ru.jamsys.rate.limit.RateLimitMax;
import ru.jamsys.rate.limit.RateLimitTps;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * RateLimitItem целевого механизма по удалению элементов - нет
 * Так как если в runTime будут выставлены для кого-то лимиты, а потом этот объект
 * Временно остановится и вместе с собой удалит установленные лимиты, то после восстановления работы
 * Ранее установленные лимиты будут утрачены? Это как-то странно?
 * Можно только управлять статусов active = true/false для отрисовки статистики
 * */

@SuppressWarnings("unused")
@Component
public class RateLimitManager implements StatisticsCollectorComponent {

    Map<String, RateLimit> map = new ConcurrentHashMap<>();

    public <T extends RateLimit> boolean contains(Class<?> clsOwner, Class<T> clsRateLimit, String key) {
        String complexKey = getRateLimitKey(clsOwner, clsRateLimit, key);
        return map.containsKey(complexKey);
    }

    public <T extends RateLimit> String getRateLimitKey(Class<?> clsOwner, Class<T> clsRateLimit, String key) {
        return clsOwner.getSimpleName() + "." + clsRateLimit.getSimpleName() + "." + key;
    }

    public <T extends RateLimit> T get(Class<?> clsOwner, Class<T> clsRateLimit, String key) {
        //RateLimitManagerKey complexKey = new RateLimitManagerKey(rateLimitGroup, clsOwner, clsRateLimit, key);
        String complexKey = getRateLimitKey(clsOwner, clsRateLimit, key);
        if (!map.containsKey(complexKey)) {
            if (clsRateLimit.equals(RateLimitTps.class)) {
                map.put(complexKey, new RateLimitTps());
            } else if (clsRateLimit.equals(RateLimitMax.class)) {
                map.put(complexKey, new RateLimitMax());
            }
        }
        @SuppressWarnings("unchecked")
        T result = (T) map.get(complexKey);
        return result;
    }

    public void reset() {
        //Используйте только для тестирования
        map.clear();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, map, new String[0], (String complexKey, RateLimit rateLimit) -> {
            if (rateLimit.isActive()) {
                result.add(new Statistic(parentTags, parentFields)
                        .addTag("index", complexKey)
                        .addFields(rateLimit.flush())
                );
            }
        });
        result.add(new Statistic(parentTags, parentFields)
                .addTag("index", getClass().getSimpleName())
                .addField("size", map.size()));
        return result;
    }

}
