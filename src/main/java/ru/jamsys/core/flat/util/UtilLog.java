package ru.jamsys.core.flat.util;

import ru.jamsys.core.component.manager.item.LogHeader;
import ru.jamsys.core.component.manager.item.LogType;

public class UtilLog {

    public static LogHeader error(Class<?> cls, Object data) {
        return new LogHeader(LogType.ERROR, cls, data);
    }

    public static LogHeader info(Class<?> cls, Object data) {
        return new LogHeader(LogType.INFO, cls, data);
    }

    public static LogHeader debug(Class<?> cls, Object data) {
        return new LogHeader(LogType.DEBUG, cls, data);
    }

    public static LogHeader printError(Class<?> cls, Object data) {
        LogHeader logHeader = new LogHeader(LogType.ERROR, cls, data);
        logHeader.print();
        return logHeader;
    }

    public static LogHeader printInfo(Class<?> cls, Object data) {
        LogHeader logHeader = new LogHeader(LogType.INFO, cls, data);
        logHeader.print();
        return logHeader;
    }

    public static LogHeader printDebug(Class<?> cls, Object data) {
        LogHeader logHeader = new LogHeader(LogType.DEBUG, cls, data);
        logHeader.print();
        return logHeader;
    }

}
