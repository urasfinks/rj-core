package ru.jamsys.component.general;

import ru.jamsys.component.general.addable.AddableCollectionItem;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.TimeController;

//MO - MapObject
//MOI - MapObjectItem
//MOIE - MapObjectItemEnvelop
public abstract class AbstractComponentCollection<
        MO extends AddableCollectionItem<MOI, MOIE> & Closable & TimeController & StatisticsCollector,
        MOI,
        MOIE
        > extends AbstractComponent<MO> {

    public MOIE add(String key, MOI object) throws Exception {
        return getItem(key).add(object);
    }

}
