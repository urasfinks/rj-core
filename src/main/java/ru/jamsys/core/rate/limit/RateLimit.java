package ru.jamsys.core.rate.limit;

import lombok.Getter;
import ru.jamsys.core.extension.CheckClassItem;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.StatisticsCollectorMap;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Прослойка RateLimit сделана, что бы смотреть в run-time изменение показателей
// Если статистики не надо, то можете просто использовать PropertyComponent для хранения настроек

@Getter
@SuppressWarnings("unused")
public class RateLimit
        extends ExpirationMsMutableImpl
        implements
        StatisticsCollectorMap<RateLimitItem>,
        Closable,
        CheckClassItem,
        ClassName,
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

    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
            sb.append(key).append(": ").append(map.get(key).getMomentumStatistic());
        }
        return sb.toString();
    }

    public void reset() {
        // Рекомендуется использовать только для тестов
        map.forEach((String key, RateLimitItem rateLimitItem) -> rateLimitItem.reset());
    }

    public boolean check(Integer limit) {
        for (String key : map.keySet()) {
            if (!map.get(key).check(limit)) {
                return false;
            }
        }
        return true;
    }

    public RateLimitItem get(String name) {
        return map.get(name);
    }

    public RateLimit init(String name, RateLimitItemInstance rateLimitItemInstance) {
        map.computeIfAbsent(name, key -> rateLimitItemInstance.create(getClassName() + "." + index + "." + key));
        return this;
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
        return true;
    }

}
