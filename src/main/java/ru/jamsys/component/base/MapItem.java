package ru.jamsys.component.base;

import ru.jamsys.extension.Closable;
import ru.jamsys.statistic.TimeController;

//MO - MapObject
//MOI - MapObjectItem
//MOIW - MapObjectItemWrap
public abstract class MapItem<
        MO extends AddableMapElement<String, MOI, MOIW> & Closable & TimeController,
        MOI,
        MOIW
        > extends MapBase<MO> {

    public MOIW add(String key, String key2, MOI object) throws Exception {
        return get(key).add(key2, object);
    }

}
