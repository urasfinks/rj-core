package ru.jamsys.task.handler;

import ru.jamsys.task.Task;

import java.util.concurrent.atomic.AtomicBoolean;

public interface TaskHandler {

    void run(Task task, AtomicBoolean isRun) throws Exception;

    long getTimeoutMs();

}
