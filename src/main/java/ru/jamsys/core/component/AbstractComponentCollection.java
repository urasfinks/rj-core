package ru.jamsys.core.component;

import ru.jamsys.core.extension.addable.AddableCollectionItem;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.time.TimeControllerMs;

//MO - MapObject
//MOI - MapObjectItem
//MOIE - MapObjectItemEnvelop
public abstract class AbstractComponentCollection<
        MO extends AddableCollectionItem<MOI, MOIE> & Closable & TimeControllerMs & StatisticsFlush,
        MOI,
        MOIE
        > extends AbstractComponent<MO> {

    public MOIE add(String key, MOI object) throws Exception {
        return getItem(key).add(object);
    }

}