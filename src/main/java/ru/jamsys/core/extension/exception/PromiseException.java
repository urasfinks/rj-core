package ru.jamsys.core.extension.exception;

import lombok.Getter;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.line.writer.LineWriter;
import ru.jamsys.core.extension.line.writer.LineWriterString;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.promise.Promise;

import java.io.PrintStream;
import java.io.PrintWriter;

@Getter
public class PromiseException extends RuntimeException {

    Promise promise;

    AbstractPromiseTask promiseTask;

    Throwable throwable;

    public PromiseException(Promise promise, AbstractPromiseTask promiseTask, Throwable throwable) {
        this.promise = promise;
        this.promiseTask = promiseTask;
        this.throwable = throwable;
    }

    @Override
    public String getMessage() {
        return throwable.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return throwable.getLocalizedMessage();
    }

    @Override
    public synchronized Throwable getCause() {
        return throwable.getCause();
    }

    @Override
    public String toString() {
        LineWriter lineWriter = new LineWriterString();
        lineWriter.addLine("Promise: " + getPromise().getIndex());
        if (getPromiseTask() != null) {
            lineWriter.addLine("Task: " + getPromiseTask().getNs());
        }
        return ExceptionHandler.getTextException(getThrowable(), lineWriter);
    }

    @Override
    public void printStackTrace() {
        throwable.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        throwable.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        throwable.printStackTrace(s);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return throwable.getStackTrace();
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        throwable.setStackTrace(stackTrace);
    }

}
