package ru.jamsys.core.component.manager;

import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

//MO - MapObject
//MOI - MapObjectItem

// Коркас менаджера, у которого есть Map дочерних объектов типа Map
public abstract class AbstractManagerMapItem<
        MO extends AddToMap<String, MOI> & Closable & ExpirationMsMutable & StatisticsFlush,
        MOI
        > extends AbstractManager<MO> {

    public void add(String key, String key2, MOI object) throws Exception {
        get(key).add(key2, object);
    }

}
