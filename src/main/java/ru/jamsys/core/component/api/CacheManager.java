package ru.jamsys.core.component.api;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractComponentMap;

import ru.jamsys.core.component.item.Cache;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;

// MO - MapObject
// MOI - MapObjectItem

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class CacheManager<MOI> extends AbstractComponentMap<
        Cache<String, MOI>,
        ExpiredMsMutableEnvelope<MOI>
        > implements KeepAliveComponent, StatisticsFlushComponent {

    @Override
    public Cache<String, MOI> build(String index) {
        return new Cache<>(index);
    }
}
