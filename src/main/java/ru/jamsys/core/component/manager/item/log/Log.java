package ru.jamsys.core.component.manager.item.log;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceLoggerRemote;
import ru.jamsys.core.extension.ByteSerialization;

import java.io.PrintStream;
import java.util.Map;

public interface Log extends ByteSerialization {

    LogType getLogType();

    String getView();

    String getData();

    Map<String, String> getHeader();

    long getTimeAdd();

    default Log print() {
        PrintStream ps = getLogType().equals(LogType.ERROR) ? System.err : System.out;
        ps.println(getView());
        return this;
    }

    default Log sendRemote() {
        App.get(ServiceLoggerRemote.class).add(this);
        return this;
    }

}
