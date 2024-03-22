package ru.jamsys.component;

import lombok.NonNull;
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

    public static class ComplexKey {
        final RateLimitGroup rateLimitGroup;
        final Class<?> cls;
        final String key;

        public ComplexKey(@NonNull RateLimitGroup rateLimitGroup, @NonNull Class<?> cls, @NonNull String key) {
            this.rateLimitGroup = rateLimitGroup;
            this.cls = cls;
            this.key = key;
        }

        @Override
        public String toString() {
            return rateLimitGroup.getName() + "." + cls.getSimpleName() + "." + key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ComplexKey that = (ComplexKey) o;

            if (rateLimitGroup != that.rateLimitGroup) return false;
            if (!cls.equals(that.cls)) return false;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            int result = rateLimitGroup.hashCode();
            result = 31 * result + cls.hashCode();
            result = 31 * result + key.hashCode();
            return result;
        }
    }

    Map<ComplexKey, RateLimitItem> map = new ConcurrentHashMap<>();

    public boolean contains(RateLimitGroup rateLimitGroup, Class<?> cls, String key) {
        ComplexKey complexKey = new ComplexKey(rateLimitGroup, cls, key);
        return map.containsKey(complexKey);
    }

    public RateLimitItem get(RateLimitGroup rateLimitGroup, Class<?> cls, String key) {
        ComplexKey complexKey = new ComplexKey(rateLimitGroup, cls, key);
        /*
            Pool.ThreadPool.KeepAliveTask
            Pool.ThreadPool.FlushStatisticCollectorTask
            Pool.ThreadPool.ReadStatisticSecTask

            Thread.ThreadPool.KeepAliveTask
            Thread.ThreadPool.FlushStatisticCollectorTask
            Thread.ThreadPool.ReadStatisticSecTask

            Broker.BrokerQueue.KeepAliveTask
            Broker.BrokerQueue.FlushStatisticCollectorTask
            Broker.BrokerQueue.ReadStatisticSecTask
            Broker.BrokerQueue.StatisticSec
        * */
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
        Util.riskModifierMap(isRun, map, new ComplexKey[0], (ComplexKey complexKey, RateLimitItem rateLimitItem) -> {
            if (rateLimitItem.isActive()) {
                result.add(new Statistic(parentTags, parentFields)
                        .addTag("index", complexKey.toString())
                        .addTag("group", complexKey.rateLimitGroup.getName())
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
