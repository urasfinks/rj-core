package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.cache.Cache;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class CacheManager implements KeepAliveComponent, StatisticsCollectorComponent {

    Map<String, Cache<?, ?>> map = new ConcurrentHashMap<>();

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        Util.riskModifierMap(isRun, map, new String[0], (String key, Cache<?, ?> cache) -> cache.keepAlive(isRun));
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(threadEnvelope.getIsWhile(), map, new String[0], (String key, Cache<?, ?> cache) -> {
            Map<String, String> parentTagsNew = new LinkedHashMap<>(parentTags);
            parentTagsNew.put("KeyCache", key);
            result.addAll(cache.flushAndGetStatistic(parentTagsNew, parentFields, threadEnvelope));
        });
        return result;
    }

}
