package ru.jamsys.core.component;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.exception.PromiseException;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.extension.line.writer.LineWriterString;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;

@Setter
@Component
@Lazy
public class ExceptionHandler extends AnnotationPropertyExtractor {

    @SuppressWarnings("all")
    private static int maxLine = 50;

    @PropertyName("log.uploader.remote")
    private Boolean remote = false;

    @PropertyName("run.args.console.output")
    private Boolean consoleOutput = true;

    public ExceptionHandler(ApplicationContext applicationContext) {
        new PropertyDispatcher(
                applicationContext.getBean(ServiceProperty.class),
                null,
                this,
                null
        );
    }

    public void handler(Throwable th) {
        if (consoleOutput) {
            LineWriter lineWriter = new LineWriterString();
            if (th instanceof PromiseException promiseException) {
                Util.logConsole(getClass(), promiseException.toString());
            } else {
                Util.logConsole(getClass(), getTextException(th, lineWriter));
            }
        }
        if (remote) {
            LineWriterList lineWriterList = new LineWriterList();
            lineWriterList.addLine(
                    UtilDate.msFormat(System.currentTimeMillis()) + " " + Thread.currentThread().getName()
            );
            getTextException(th, lineWriterList);
            App.get(ServiceLogger.class).add(
                    LogType.SYSTEM_EXCEPTION,
                    new HashMapBuilder<String, Object>().append("exception", lineWriterList.getResult()),
                    LogType.SYSTEM_EXCEPTION.getNameCamel(),
                    false
            );
        }

    }

    public static String getTextException(Throwable th, LineWriter sw) {
        printStackTrace(th, sw, (th instanceof ForwardException) ? 1 : null);
        Throwable cause = th.getCause();
        if (cause != null) {
            sw.addLine("Caused by: ");
            getTextException(cause, sw);
        }
        return sw.toString();
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
