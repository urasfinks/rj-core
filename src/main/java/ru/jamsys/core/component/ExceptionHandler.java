package ru.jamsys.core.component;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.extension.line.writer.LineWriterString;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyName;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.flat.util.Util;

import java.util.Set;

@Setter
@Component
@Lazy
public class ExceptionHandler extends PropertyConnector implements PropertySubscriberNotify {

    private int maxLine = 20;

    @PropertyName("remote.log")
    private String remoteLog = "true";

    @PropertyName("console.output")
    private String consoleOutput = "true";

    public ExceptionHandler(ApplicationContext applicationContext) {
        applicationContext
                .getBean(ServiceProperty.class)
                .getSubscriber(this, this, "run.args");
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
                    LogType.SYSTEM_EXCEPTION.getName(),
                    false
            );
        }

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

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {

    }

}
