package ru.jamsys.core.rate.limit;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.*;
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
        Closable,
        CheckClassItem,
        ClassName,
        LifeCycleInterface,
        AddToMap<String, RateLimitItem>
{

    final Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    private final String index;

    public RateLimit(String index) {
        this.index = index;
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, RateLimitItem> getMapForFlushStatistic() {
        return map;
    }

    public boolean check(Integer limit) {
        for (String key : map.keySet()) {
            if (!map.get(key).check(limit)) {
                Util.logConsole("RateLimit index: " + index + "; key: " + key + " FAILED (" + map.get(key).get() + " ? " + limit + ")", true);
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
                applicationContext,
                getClassName(applicationContext) + "." + index + "." + key)
        );
        return this;
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
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
