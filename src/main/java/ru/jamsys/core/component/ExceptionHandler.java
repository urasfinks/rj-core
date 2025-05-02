package ru.jamsys.core.component;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilLog;

@Setter
@Component
@Lazy
public class ExceptionHandler extends AnnotationPropertyExtractor<Object> {

    @SuppressWarnings("all")
    private static int maxLine = 50;

    public ExceptionHandler(ApplicationContext applicationContext) {
        new PropertyDispatcher<>(
                applicationContext.getBean(ServiceProperty.class),
                null,
                this,
                null
        ).run();
    }

    public void handler(Throwable th) {
        LineWriterList lineWriterList = new LineWriterList();
        lineWriterList.addLine(
                UtilDate.msFormat(System.currentTimeMillis()) + " " + Thread.currentThread().getName()
        );
        getTextException(th, lineWriterList);
        UtilLog
                .error(lineWriterList.getResult())
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
