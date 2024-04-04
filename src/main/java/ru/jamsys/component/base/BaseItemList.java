package ru.jamsys.component.base;

import ru.jamsys.extension.AddableComponentItemList;
import ru.jamsys.extension.Closable;
import ru.jamsys.statistic.TimeController;

//MO - MapObject
//MOI - MapObjectItem
//MOIW - MapObjectItemWrap
public abstract class BaseItemList<
        MO extends AddableComponentItemList<MOI, MOIW> & Closable & TimeController,
        MOI,
        MOIW
        > extends Base<MO> {

    public MOIW add(String key, MOI object) throws Exception {
        return get(key).add(object);
    }

}
