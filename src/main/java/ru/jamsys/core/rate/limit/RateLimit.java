package ru.jamsys.core.rate.limit;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsCollectorMap;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Прослойка RateLimit сделана, не для того, что бы отслеживать в run-time изменение показателей
// RateLimit даёт комплексный подход к гибким метрикам ограничения скорости например:
// установить 1 tps + не больше 200 запросов в день

@Getter
@SuppressWarnings("unused")
public class RateLimit
        extends ExpirationMsMutableImpl
        implements
        StatisticsCollectorMap<RateLimitItem>,
        ClassEquals,
        UniqueClassName,
        LifeCycleInterface,
        AddToMap<String, RateLimitItem>
{

    final Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    private final String index;

    public RateLimit(String index) {
        this.index = index;
    }

    @Override
    public Map<String, RateLimitItem> getMapForFlushStatistic() {
        return map;
    }

    public boolean check() {
        for (String key : map.keySet()) {
            if (!map.get(key).check()) {
                Util.logConsole("RateLimit [ABORT] index: " + index + "; key: " + key + "; max: " + map.get(key).max()+"; now: "+map.get(key).get()+";", true);
                return false;
            }
        }
        return true;
    }

    public RateLimitItem get(String name) {
        return map.get(name);
    }

    public RateLimit init(ApplicationContext applicationContext, String name, RateLimitFactory rateLimitFactory) {
        map.computeIfAbsent(name, key -> rateLimitFactory.create(
                getClassName(applicationContext) + "." + index + "." + key)
        );
        return this;
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return true;
    }

    @Override
    public void run() {
        // Пока ничего не надо
    }

    @Override
    public void shutdown() {
        // Пока ничего не надо
    }

}
