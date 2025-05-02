package ru.jamsys.core.flat.util;

import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.manager.item.log.Log;
import ru.jamsys.core.component.manager.item.log.LogType;

public class UtilLog {

    public static Log error(Object data) {
        return new Log(LogType.ERROR, getCaller(), data);
    }

    public static Log info(Object data) {
        return new Log(LogType.INFO, getCaller(), data);
    }

    public static Log debug(Object data) {
        return new Log(LogType.DEBUG, getCaller(), data);
    }

    public static void printError(Object data) {
        new Log(LogType.ERROR, getCaller(), data).print();

    }

    public static void printInfo(Object data) {
        new Log(LogType.INFO, getCaller(), data).print();
    }

    @SuppressWarnings("unused")
    public static void printDebug(Object data) {
        new Log(LogType.DEBUG, getCaller(), data).print();
    }

    public static void printAction(String action) {
        new Log(LogType.INFO, getCaller(), null).addHeader("action", action).print();
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
