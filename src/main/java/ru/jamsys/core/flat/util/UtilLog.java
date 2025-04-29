package ru.jamsys.core.flat.util;

import ru.jamsys.core.component.manager.item.log.Log;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.component.manager.item.log.PersistentDataHeader;

public class UtilLog {

    public static PersistentDataHeader error(Class<?> cls, Object data) {
        return new Log(LogType.ERROR, cls, data);
    }

    public static PersistentDataHeader info(Class<?> cls, Object data) {
        return new Log(LogType.INFO, cls, data);
    }

    public static PersistentDataHeader debug(Class<?> cls, Object data) {
        return new Log(LogType.DEBUG, cls, data);
    }

    public static Log printError(Class<?> cls, Object data) {
        Log log = new Log(LogType.ERROR, cls, data);
        log.print();
        return log;
    }

    public static PersistentDataHeader printInfo(Class<?> cls, Object data) {
        Log log = new Log(LogType.INFO, cls, data);
        log.print();
        return log;
    }

    @SuppressWarnings("unused")
    public static PersistentDataHeader printDebug(Class<?> cls, Object data) {
        Log log = new Log(LogType.DEBUG, cls, data);
        log.print();
        return log;
    }

}
