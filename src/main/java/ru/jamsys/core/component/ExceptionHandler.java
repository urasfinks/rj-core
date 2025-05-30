package ru.jamsys.core.component;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilLog;

@Setter
@Component
@Lazy
public class ExceptionHandler {

    @SuppressWarnings("all")
    private static int maxLine = 50;

    public void handler(Throwable th, Object context) {
        LineWriterList lineWriterList = new LineWriterList();
        lineWriterList.addLine(
                UtilDate.msFormat(System.currentTimeMillis()) + " " + Thread.currentThread().getName()
        );
        getTextException(th, lineWriterList);
        UtilLog
                .error(context == null
                        ? lineWriterList.getResult()
                        : new HashMapBuilder<String, Object>()
                        .append("context", context)
                        .append("exception", lineWriterList.getResult())
                )
                .print();
    }

    public static void getTextException(Throwable th, LineWriter sw) {
        printStackTrace(th, sw, (th instanceof ForwardException) ? 1 : null);
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

    private static void printStackTrace(Throwable th, LineWriter sw, Integer count) {
        StackTraceElement[] elements = th.getStackTrace();
        sw.addLine(th.getClass().getName() + ": " + th.getMessage());
        int m = count != null ? count : maxLine;
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
