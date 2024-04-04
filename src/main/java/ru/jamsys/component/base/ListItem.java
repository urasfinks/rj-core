package ru.jamsys.component.base;

import ru.jamsys.extension.Closable;
import ru.jamsys.statistic.TimeController;

//MO - MapObject
//MOI - MapObjectItem
//MOIW - MapObjectItemWrap
public abstract class ListItem<
        MO extends AddableListElement<MOI, MOIW> & Closable & TimeController,
        MOI,
        MOIW
        > extends MapBase<MO> {

    public MOIW add(String key, MOI object) throws Exception {
        return get(key).add(object);
    }

}
