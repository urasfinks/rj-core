package ru.jamsys.core.i360.entity;

import ru.jamsys.core.flat.util.UtilJson;

// Единица информации (объект)
public interface Entity {

    String getUuid();

    static Entity newInstance(String json, Class<? extends Entity> cls) throws Throwable {
        return UtilJson.toObject(json, cls);
    }

}
