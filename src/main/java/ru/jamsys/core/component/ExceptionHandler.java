package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class ExceptionHandler {

    public void handler(Throwable e) {
        StringWriter sw = new StringWriter();
        sw.append(Thread.currentThread().getName());
        sw.append("\n");
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        System.err.println(sw);
    }

}
