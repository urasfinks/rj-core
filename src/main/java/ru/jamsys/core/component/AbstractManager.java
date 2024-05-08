package ru.jamsys.core.component;

import lombok.Setter;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutable;
import ru.jamsys.core.util.UtilRisc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractManager<MO extends Closable & ExpiredMsMutable & StatisticsFlush>
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

    @SuppressWarnings("unused")
    public MO get(String key) {
        return map.computeIfAbsent(key, _ -> build(key));
    }

    public void put(String key, MO object) {
        map.computeIfAbsent(key, _ -> object);
    }

    //Используйте только для тестирования
    @SuppressWarnings("unused")
    public void clear() {
        map.clear();
    }

    @SuppressWarnings("unused")
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

}
