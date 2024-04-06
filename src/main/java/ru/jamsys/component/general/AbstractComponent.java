package ru.jamsys.component.general;

import ru.jamsys.extension.*;
import ru.jamsys.statistic.TimeController;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractComponent<MO extends Closable & TimeController & StatisticsCollector>
        implements
        StatisticsCollectorMap<MO>,
        KeepAlive,
        ComponentItemBuilder<MO> {

    protected final Map<String, MO> map = new ConcurrentHashMap<>();

    @Override
    public Map<String, MO> getMapStatisticCollectorMap() {
        return map;
    }

    @Override
    public void keepAlive(ThreadEnvelope threadEnvelope) {
        Util.riskModifierMap(
                threadEnvelope.getIsWhile(),
                map,
                new String[0],
                (String key, MO element) -> {
                    if (element.isExpired()) {
                        map.remove(key);
                        element.close();
                    } else if (element instanceof KeepAlive) {
                        ((KeepAlive) element).keepAlive(threadEnvelope);
                    }
                }
        );
    }

    @SuppressWarnings("unused")
    public MO getItem(String key) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!map.containsKey(key)) {
            map.putIfAbsent(key, build(key));
        }
        return map.get(key);
    }

    //Используйте только для тестирования
    @SuppressWarnings("unused")
    public void reset() {
        map.clear();
    }

    @SuppressWarnings("unused")
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

}
