package ru.jamsys.component.general;

import ru.jamsys.component.general.addable.AddableMapItem;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.TimeController;

//MO - MapObject
//MOI - MapObjectItem
public abstract class AbstractComponentMap<
        MO extends AddableMapItem<String, MOI> & Closable & TimeController & StatisticsCollector,
        MOI
        > extends AbstractComponent<MO> {

    public void add(String key, String key2, MOI object) throws Exception {
        getItem(key).add(key2, object);
    }

}
