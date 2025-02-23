package ru.jamsys.core.rate.limit;

import lombok.Getter;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsCollectorMap;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.extension.exception.RateLimitException;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// RateLimit - набор RateLimitItem
// Так как это набор, можно добавить RateLimitItem разных типов, на подобии TPS + TPD
// Успехом считается если все правила сказали ОК

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

    private final AtomicBoolean run = new AtomicBoolean(false);

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
        UtilRisc.forEach(null, map, (key, rateLimitItem) -> {
            if (rateLimitItem.get() >= rateLimitItem.max()) {
                throw new RateLimitException(
                        "RateLimit ABORT",
                        "prop: " + key
                                + "; max: " + rateLimitItem.max()
                                + "; now: " + rateLimitItem.get()
                                + "; key: " + rateLimitItem.getKey()
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
                    Util.logConsole(
                            getClass(),
                            "RateLimit [ABORT] key: " + key
                                    + "; max: " + rateLimitItem.max()
                                    + "; now: " + rateLimitItem.get()
                                    + "; key: " + rateLimitItem.getKey()
                                    + ";",
                            true
                    );
                    return false;
                }
            }
        }
        return true;
    }

    public RateLimitItem get(String key) {
        RateLimitItem rateLimitItem = map.get(key);
        if (rateLimitItem == null) {
            throw new RuntimeException("available: " + UtilJson.toStringPretty(map, "{}"));
        }
        return rateLimitItem;
    }

    public RateLimitItem computeIfAbsent(String key, RateLimitFactory rateLimitFactory) {
        return map.computeIfAbsent(key, s -> {
            RateLimitItem rateLimitItem = rateLimitFactory.create(getCascadeName(s));
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
