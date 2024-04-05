package ru.jamsys.component.base;

import ru.jamsys.extension.AddableComponentItemMap;
import ru.jamsys.extension.Closable;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.TimeController;

//MO - MapObject
//MOI - MapObjectItem
//MOIW - MapObjectItemWrap
public abstract class BaseItemMap<
        MO extends AddableComponentItemMap<String, MOI, MOIW> & Closable & TimeController & StatisticsCollector,
        MOI,
        MOIW
        > extends Base<MO> {

    public MOIW add(String key, String key2, MOI object) throws Exception {
        return get(key).add(key2, object);
    }

}
