package ru.jamsys.core.component;

import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.time.TimeControllerMs;

//MO - MapObject
//MOI - MapObjectItem
public abstract class AbstractComponentMap<
        MO extends AddToMap<String, MOI> & Closable & TimeControllerMs & StatisticsFlush,
        MOI
        > extends AbstractComponent<MO> {

    public void add(String key, String key2, MOI object) throws Exception {
        getItem(key).add(key2, object);
    }

}
