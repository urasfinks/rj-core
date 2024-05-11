package ru.jamsys.core.component;

import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.time.mutable.ExpirationMsMutable;

//MO - MapObject
//MOI - MapObjectItem
//MOIE - MapObjectItemEnvelop

// Коркас менаджера, у которого есть Map дочерних объектов типа List
public abstract class AbstractManagerListItem<
        MO extends AddToList<MOI, MOIE> & Closable & ExpirationMsMutable & StatisticsFlush,
        MOI,
        MOIE
        > extends AbstractManager<MO> {

    public MOIE add(String key, MOI object) throws Exception {
        return get(key).add(object);
    }

}
