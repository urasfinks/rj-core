package ru.jamsys.core.rate.limit;

import lombok.Getter;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsCollectorMap;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.extension.exception.RateLimitException;
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
        CascadeName,
        LifeCycleInterface,
        AddToMap<String, RateLimitItem> {

    final Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    @Getter
    private final String key;

    @Getter
    private final CascadeName parentCascadeName;

    public RateLimit(CascadeName parentCascadeName, String key) {
        this.parentCascadeName = parentCascadeName;
        this.key = key;
    }

    @Override
    public Map<String, RateLimitItem> getMapForFlushStatistic() {
        return map;
    }

    public void checkOrThrow() {
        for (String key : map.keySet()) {
            RateLimitItem rateLimitItem = map.get(key);
            if (rateLimitItem.get() >= rateLimitItem.max()) {
                throw new RateLimitException(
                        "RateLimit ABORT",
                        "key: " +  map.get(key).getKey() + "; max: " + map.get(key).max() + "; now: " + map.get(key).get() + ";"
                );
            }
        }
    }

    public boolean check() {
        for (String key : map.keySet()) {
            if (!map.get(key).check()) {
                Util.logConsole(
                        getClass(),
                        "RateLimit [ABORT] key: " + map.get(key).getKey() + "; max: " + map.get(key).max() + "; now: " + map.get(key).get() + ";",
                        true
                );
                return false;
            }
        }
        return true;
    }

    public RateLimitItem get(String key) {
        return map.get(key);
    }

    public RateLimit init(String key, RateLimitFactory rateLimitFactory) {
        map.computeIfAbsent(getCascadeName(key), rateLimitFactory::create);
        return this;
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return true;
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {

    }

}
