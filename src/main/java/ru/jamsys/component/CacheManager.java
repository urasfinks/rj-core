package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.component.item.Cache;
import ru.jamsys.component.base.MapItem;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

// MO - MapObject
// MOI - MapObjectItem

@SuppressWarnings("unused")
@Component
@Lazy
public class CacheManager<MOI> extends MapItem<
        Cache<String, MOI>,
        TimeEnvelope<MOI>,
        TimeEnvelope<MOI>
        > implements KeepAliveComponent, StatisticsCollectorComponent {

    @Override
    public void keepAlive(ThreadEnvelope threadEnvelope) {
        Util.riskModifierMap(threadEnvelope.getIsWhile(), map, new String[0], (String key, Cache<String, MOI> cache) -> cache.keepAlive(threadEnvelope));
    }

    @Override
    public Cache<String, MOI> build(String key) {
        return new Cache<>();
    }
}
