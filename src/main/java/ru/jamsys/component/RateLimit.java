package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.extension.RateLimitKey;
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

    Map<RateLimitKey, RateLimitItem> map = new ConcurrentHashMap<>();

    public boolean contains(RateLimitGroup rateLimitGroup, Class<?> cls, String key) {
        RateLimitKey complexKey = new RateLimitKey(rateLimitGroup, cls, key);
        return map.containsKey(complexKey);
    }

    public RateLimitItem get(RateLimitGroup rateLimitGroup, Class<?> cls, String key) {
        RateLimitKey complexKey = new RateLimitKey(rateLimitGroup, cls, key);
        if (!map.containsKey(complexKey)) {
            map.put(complexKey, new RateLimitItem());
        }
        return map.get(complexKey);
    }

    public void reset() {
        //Используйте только для тестирования
        map.clear();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, map, new RateLimitKey[0], (RateLimitKey complexKey, RateLimitItem rateLimitItem) -> {
            if (rateLimitItem.isActive()) {
                result.add(new Statistic(parentTags, parentFields)
                        .addTag("index", complexKey.getKey())
                        .addTag("group", complexKey.getRateLimitGroup().getName())
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
