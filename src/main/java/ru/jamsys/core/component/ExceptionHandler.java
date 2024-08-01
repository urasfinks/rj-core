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
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.extension.line.writer.LineWriterString;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.flat.util.Util;

@Setter
@Component
@Lazy
public class ExceptionHandler extends RepositoryPropertiesField {

    private int maxLine = 50;

    @PropertyName("run.args.remote.log")
    private String remoteLog = "true";

    @PropertyName("run.args.console.output")
    private String consoleOutput = "true";

    public ExceptionHandler(ApplicationContext applicationContext) {
        applicationContext
                .getBean(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgent(null, this, null, true);
    }

    public void handler(Throwable th) {
        if (consoleOutput.equals("true")) {
            LineWriter lineWriter = new LineWriterString();
            Util.logConsole(getTextException(th, lineWriter));
        }
        if (remoteLog.equals("true")) {
            LineWriterList lineWriterList = new LineWriterList();
            lineWriterList.addLine(
                    Util.msToDataFormat(System.currentTimeMillis()) + " " + Thread.currentThread().getName()
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

    public String getTextException(Throwable th, LineWriter sw) {
        printStackTrace(th, sw, (th instanceof ForwardException) ? 1 : null);
        Throwable cause = th.getCause();
        if (cause != null) {
            sw.addLine("Caused by: ");
            getTextException(cause, sw);
        }
        return sw.toString();
    }

    private void printStackTrace(Throwable th, LineWriter sw, Integer count) {
        StackTraceElement[] elements = th.getStackTrace();
        sw.addLine(th.getClass().getName() + ": " + th.getMessage());
        int m = count != null ? count : maxLine;
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
