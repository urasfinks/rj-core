package ru.jamsys.task.handler;

import ru.jamsys.task.Task;

import java.util.concurrent.atomic.AtomicBoolean;

public interface TaskHandler <T extends Task> {

    void run(T task, AtomicBoolean isRun) throws Exception;

    long getTimeoutMs();

}
