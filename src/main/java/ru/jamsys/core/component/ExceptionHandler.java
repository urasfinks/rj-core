package ru.jamsys.core.component;

import lombok.Setter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.flat.util.date.UtilDate;
import ru.jamsys.core.flat.util.UtilLog;

@Setter
public class ExceptionHandler {

    @SuppressWarnings("all")
    private static int maxLine = 50;

    public static void handler(Throwable th, Object context) {
        UtilLog.error(getExceptionObject(th, context)).print();
    }

    public static Object getExceptionObject(Throwable th, Object context) {
        LineWriterList lineWriterList = new LineWriterList();
        lineWriterList.addLine(
                UtilDate.millis(System.currentTimeMillis()).toDate().getDate() + " " + Thread.currentThread().getName()
        );
        getTextException(th, lineWriterList);
        return context == null
                ? lineWriterList.getResult()
                : new HashMapBuilder<String, Object>()
                .append("context", context)
                .append("exception", lineWriterList.getResult());
    }

    public static void getTextException(Throwable th, LineWriter sw) {
        printStackTrace(th, sw);
        Throwable cause = th.getCause();
        if (cause != null) {
            sw.addLine("Caused by: ");
            getTextException(cause, sw);
        }
    }

    public static String getLineStack(StackTraceElement element){
        return "at "
                + element.getClassName() + "." + element.getMethodName()
                + "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
    }

    private static void printStackTrace(Throwable th, LineWriter sw) {
        int m = maxLine;
        StackTraceElement[] elements = th.getStackTrace();
        sw.addLine(th.getClass().getName() + ": " + th.getMessage());
        if (th instanceof ForwardException forwardException) {
            m = forwardException.getLine();
            if (forwardException.getContextSnapshot() != null) {
                sw.addLineAll(forwardException.getContextSnapshot());
            }
        }
        int s = elements.length;
        for (StackTraceElement element : elements) {
            sw.addLineIndent(getLineStack(element));
            m--;
            s--;
            if (m == 0) {
                sw.addLineIndent("... " + s + " more");
                break;
            }
        }
    }

}
