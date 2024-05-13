package ru.jamsys.core.component.manager;

import lombok.Setter;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;
import ru.jamsys.core.util.UtilRisc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class AbstractManager<MO extends Closable & ExpirationMsMutable & StatisticsFlush>
        implements
        StatisticsCollectorMap<MO>,
        KeepAlive,
        ComponentItemBuilder<MO> {

    protected final Map<String, MO> map = new ConcurrentHashMap<>();

    @Setter
    protected boolean cleanableMap = true;

    @Override
    public Map<String, MO> getMapForFlushStatistic() {
        return map;
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        UtilRisc.forEach(
                isThreadRun,
                map,
                (String key, MO element) -> {
                    if (cleanableMap && element.isExpired()) {
                        map.remove(key);
                        element.close();
                    } else if (element instanceof KeepAlive) {
                        ((KeepAlive) element).keepAlive(isThreadRun);
                    }
                }
        );
    }

    public MO get(String key) {
        return map.computeIfAbsent(key, _ -> build(key));
    }

    public void put(String key, MO object) {
        map.computeIfAbsent(key, _ -> object);
    }

    public Map<String, MO> getTestMap() {
        return new HashMap<>(map);
    }

}
