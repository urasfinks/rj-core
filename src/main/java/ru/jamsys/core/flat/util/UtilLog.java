package ru.jamsys.core.flat.util;

import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.manager.item.log.Log;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.component.manager.item.log.PersistentDataHeader;

public class UtilLog {

    public static PersistentDataHeader error(Object data) {
        return new Log(LogType.ERROR, getCaller(), data);
    }

    public static PersistentDataHeader info(Object data) {
        return new Log(LogType.INFO, getCaller(), data);
    }

    public static PersistentDataHeader debug(Object data) {
        return new Log(LogType.DEBUG, getCaller(), data);
    }

    public static Log printError(Object data) {
        Log log = new Log(LogType.ERROR, getCaller(), data);
        log.print();
        return log;
    }

    public static PersistentDataHeader printInfo(Object data) {
        Log log = new Log(LogType.INFO, getCaller(), data);
        log.print();
        return log;
    }

    @SuppressWarnings("unused")
    public static PersistentDataHeader printDebug(Object data) {
        Log log = new Log(LogType.DEBUG, getCaller(), data);
        log.print();
        return log;
    }

    public static String getCaller() {
        // Получаем стек вызовов
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Элемент с индексом 0 — это getStackTrace()
        // Элемент с индексом 1 — это текущий метод (getCallerLineNumber)
        // Элемент с индексом 2 — это метод, который вызвал текущий (doSomething)
        // Элемент с индексом 3 — это метод, который вызвал doSomething (main или другой)
        if (stackTrace.length >= 4) {
            return ExceptionHandler.getLineStack(stackTrace[3]);
        }
        return null;
    }

}
