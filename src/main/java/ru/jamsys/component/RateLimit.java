package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.RateLimitGroup;
import ru.jamsys.statistic.RateLimitItem;
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
public class RateLimit implements StatisticsCollectorComponent {

    Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    public RateLimitItem get(RateLimitGroup rateLimitGroup, Class<?> cls, String key) {
        String keyItem = rateLimitGroup.getName() + "." + cls.getSimpleName() + "." + key;
        if (!map.containsKey(keyItem)) {
            map.put(keyItem, new RateLimitItem());
        }
        return map.get(keyItem);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, map, new String[0], (String key, RateLimitItem rateLimitItem) -> {
            if (rateLimitItem.isActive()) {
                result.add(new Statistic(parentTags, parentFields)
                        .addTag("index", key)
                        .addField("max", rateLimitItem.getMax())
                        .addField("tps", rateLimitItem.flushTps()));
            }
        });
        result.add(new Statistic(parentTags, parentFields)
                .addTag("index", getClass().getSimpleName())
                .addField("size", map.size()));
        return result;
    }

}
