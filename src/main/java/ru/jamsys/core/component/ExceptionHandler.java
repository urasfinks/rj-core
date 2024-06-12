package ru.jamsys.core.component;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterString;
import ru.jamsys.core.flat.util.Util;

@Setter
@Component
@Lazy
public class ExceptionHandler {

    private int maxLine = 15;

    public void handler(Throwable th) {
        LineWriter lineWriter = new LineWriterString();
        lineWriter.addLine(
                Util.msToDataFormat(System.currentTimeMillis())
                        + " Exception in thread: [" + Thread.currentThread().getName() + "]"
        );
        System.err.println(
                getTextException(new RuntimeException(th), lineWriter)
        );
    }

    public String getTextException(Throwable th, LineWriter sw) {
        printStackTrace(th, sw);
        Throwable cause = th.getCause();
        if (cause != null) {
            sw.addLine("Caused by: ");
            getTextException(cause, sw);
        }
        return sw.toString();
    }

    private void printStackTrace(Throwable th, LineWriter sw) {
        StackTraceElement[] elements = th.getStackTrace();
        sw.addLine(th.getClass().getName() + ": " + th.getMessage());
        int m = maxLine;
        int s = elements.length;
        for (StackTraceElement element : elements) {
            sw.addLineIndent("at "
                    + element.getClassName() + "." + element.getMethodName()
                    + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            m--;
            s--;
            if (m == 0) {
                sw.addLineIndent("... " + s + " more");
                break;
            }
        }
    }

}
