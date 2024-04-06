package ru.jamsys.component.general;

import ru.jamsys.component.general.addable.AddableMapItem;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.TimeController;

//MO - MapObject
//MOI - MapObjectItem
//MOIE - MapObjectItemEnvelop
public abstract class AbstractComponentMap<
        MO extends AddableMapItem<String, MOI, MOIE> & Closable & TimeController & StatisticsCollector,
        MOI,
        MOIE
        > extends AbstractComponent<MO> {

    public MOIE add(String key, String key2, MOI object) throws Exception {
        return getItem(key).add(key2, object);
    }

}
