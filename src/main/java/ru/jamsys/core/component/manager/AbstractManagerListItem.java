package ru.jamsys.core.component.manager;

import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.function.Consumer;

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

    public void setup(String key, Consumer<MO> fn) {
        fn.accept(get(key));
    }

}
