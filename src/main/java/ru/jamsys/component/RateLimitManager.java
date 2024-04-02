package ru.jamsys.component;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.rate.limit.RateLimit;
import ru.jamsys.rate.limit.RateLimitImpl;
import ru.jamsys.rate.limit.RateLimitName;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public <T extends RateLimit> String getKey(@NonNull Class<?> clsOwner, String key) {
        if (key == null) {
            return clsOwner.getSimpleName();
        } else {
            return clsOwner.getSimpleName() + "." + key;
        }
    }

    public boolean contains(@NonNull Class<?> clsOwner, @Nullable String key) {
        return map.containsKey(getKey(clsOwner, key));
    }

    public void initLimit(Class<?> clsOwner, String pool, RateLimitName rateLimitName, int max) {
        get(clsOwner, pool).get(rateLimitName).setMax(max);
    }

    public void updateLimit(String key, String item, int max) {
        if (map.containsKey(key)) {
            RateLimit rateLimit = map.get(key);
            RateLimitItem rateLimitItem = rateLimit.get(RateLimitName.valueOf(item));
            if (rateLimitItem != null) {
                rateLimitItem.setMax(max);
                return;
            }
        }
        App.context.getBean(ExceptionHandler.class)
                .handler(new RuntimeException("RateLimit " + key + " -> " + item + " not found"));
    }

    public RateLimit get(@NonNull Class<?> clsOwner, @Nullable String key) {
        String complexKey = getKey(clsOwner, key);
        if (!map.containsKey(complexKey)) {
            map.put(complexKey, new RateLimitImpl());
        }
        return map.get(complexKey);
    }

    public void reset() {
        //Используйте только для тестирования
        map.clear();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope) {
        List<Statistic> result = new ArrayList<>();
        long curTime = System.currentTimeMillis();
        Util.riskModifierMap(threadEnvelope.getIsWhile(), map, new String[0], (String key, RateLimit rateLimit) -> {
            if (rateLimit.isActive()) {
                HashMap<String, String> newParentTags = new HashMap<>(parentTags);
                newParentTags.put("RateLimitKey", key);
                result.addAll(rateLimit.flushAndGetStatistic(newParentTags, parentFields, threadEnvelope));
            }
        });
        return result;
    }

}
