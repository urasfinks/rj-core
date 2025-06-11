package ru.jamsys.core.component;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.extension.log.Log;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLog;

@Setter
@Component
@Lazy
public class ExceptionHandler {

    @SuppressWarnings("all")
    private static int maxLine = 50;

    public void handler(Throwable th, Object context) {
        getLog(th, context).print();
    }

    public Log getLog(Throwable th, Object context) {
        LineWriterList lineWriterList = new LineWriterList();
        lineWriterList.addLine(
                UtilDate.msFormat(System.currentTimeMillis()) + " " + Thread.currentThread().getName()
        );
        getTextException(th, lineWriterList);
        return UtilLog
                .error(context == null
                        ? lineWriterList.getResult()
                        : new HashMapBuilder<String, Object>()
                        .append("context", context)
                        .append("exception", lineWriterList.getResult())
                );
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
            if (forwardException.getContext() != null) {
                try {
                    String stringPretty = UtilJson.toStringPretty(forwardException.getContext(), "{}");
                    sw.addLine("ForwardException.Context:");
                    for (String str : stringPretty.split("\n")) {
                        sw.addLine(str);
                    }
                } catch (Throwable thSerialize) {
                    sw.addLine("ForwardException.Context: error serialize: " + thSerialize.getMessage());
                }
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
