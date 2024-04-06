package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.component.general.AbstractComponentMap;
import ru.jamsys.component.item.Cache;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.TimeEnvelope;

// MO - MapObject
// MOI - MapObjectItem

@SuppressWarnings("unused")
@Component
@Lazy
public class CacheManager<MOI> extends AbstractComponentMap<
        Cache<String, MOI>,
        TimeEnvelope<MOI>,
        TimeEnvelope<MOI>
        > implements KeepAliveComponent, StatisticsCollectorComponent {

    @Override
    public Cache<String, MOI> build(String key) {
        return new Cache<>();
    }
}
