package ru.jamsys.core.rate.limit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsCollectorMap;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.extension.exception.RateLimitException;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.statistic.expiration.ExpirationMs;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// RateLimit - набор RateLimitItem
// Так как это набор, можно добавить RateLimitItem разных типов, на подобии TPS + TPD
// Успехом считается если все правила сказали ОК

@JsonPropertyOrder({"key", "map"})
@Getter
@SuppressWarnings("unused")
public class RateLimit
        extends ExpirationMsMutableImpl
        implements
        StatisticsCollectorMap<RateLimitItem>,
        ClassEquals,
        LifeCycleInterface,
        AddToMap<String, RateLimitItem> {

    @JsonView(ExpirationMs.class)
    final Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    @JsonIgnore
    private final AtomicBoolean run = new AtomicBoolean(false);

    @Getter
    private final String key;

    public RateLimit(String key) {
        this.key = key;
    }

    @JsonIgnore
    @Override
    public Map<String, RateLimitItem> getMapForFlushStatistic() {
        return map;
    }

    public void checkOrThrow() {
        UtilRisc.forEach(null, map, (key, rateLimitItem) -> {
            if (rateLimitItem.getCount() >= rateLimitItem.getMax()) {
                throw new RateLimitException(
                        "RateLimit ABORT",
                        "prop: " + key
                                + "; max: " + rateLimitItem.getMax()
                                + "; now: " + rateLimitItem.getCount()
                                + "; key: " + rateLimitItem.getNamespace()
                                + ";"
                );
            }
        });
    }

    public boolean check() {
        String[] array = map.keySet().toArray(new String[0]);
        for (String key : array) {
            RateLimitItem rateLimitItem = map.get(key);
            if (rateLimitItem != null) {
                if (!rateLimitItem.check()) {
                    UtilLog.error(getClass(), "RateLimit ABORT")
                            .addHeader("key", key)
                            .addHeader("max", rateLimitItem.getMax())
                            .addHeader("count", rateLimitItem.getCount())
                            .addHeader("namespace", rateLimitItem.getNamespace())
                            .print();
                    return false;
                }
            }
        }
        return true;
    }

    public RateLimitItem get(String key) {
        RateLimitItem rateLimitItem = map.get(key);
        if (rateLimitItem == null) {
            throw new RuntimeException("Not found key: " + key + "; available: " + UtilJson.toStringPretty(map, "{}"));
        }
        return rateLimitItem;
    }

    public RateLimitItem computeIfAbsent(String key, RateLimitFactory rateLimitFactory) {
        return map.computeIfAbsent(key, s -> {
            RateLimitItem rateLimitItem = rateLimitFactory.create(this.key + CascadeName.append(s));
            rateLimitItem.run();
            return rateLimitItem;
        });
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return true;
    }

    @Override
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        UtilRisc.forEach(null, map, (s, rateLimitItem) -> {
            rateLimitItem.run();
        });
    }

    @Override
    public void shutdown() {
        UtilRisc.forEach(null, map, (s, rateLimitItem) -> {
            rateLimitItem.shutdown();
        });
    }

}
