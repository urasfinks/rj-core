package ru.jamsys.thread.handler;

import ru.jamsys.thread.task.Task;

import java.util.concurrent.atomic.AtomicBoolean;

public interface Handler<T extends Task> {

    void run(T task, AtomicBoolean isRun) throws Exception;

    long getTimeoutMs();

}
