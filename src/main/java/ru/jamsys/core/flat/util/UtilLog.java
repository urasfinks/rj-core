package ru.jamsys.core.flat.util;

import ru.jamsys.core.component.manager.item.log.PersistentDataHeader;
import ru.jamsys.core.component.manager.item.log.LogType;

public class UtilLog {

    public static PersistentDataHeader error(Class<?> cls, Object data) {
        return new PersistentDataHeader(LogType.ERROR, cls, data);
    }

    public static PersistentDataHeader info(Class<?> cls, Object data) {
        return new PersistentDataHeader(LogType.INFO, cls, data);
    }

    public static PersistentDataHeader debug(Class<?> cls, Object data) {
        return new PersistentDataHeader(LogType.DEBUG, cls, data);
    }

    public static PersistentDataHeader printError(Class<?> cls, Object data) {
        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.ERROR, cls, data);
        persistentDataHeader.print();
        return persistentDataHeader;
    }

    public static PersistentDataHeader printInfo(Class<?> cls, Object data) {
        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.INFO, cls, data);
        persistentDataHeader.print();
        return persistentDataHeader;
    }

    public static PersistentDataHeader printDebug(Class<?> cls, Object data) {
        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.DEBUG, cls, data);
        persistentDataHeader.print();
        return persistentDataHeader;
    }

}
