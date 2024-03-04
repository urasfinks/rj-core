package ru.jamsys.task;

import java.util.concurrent.atomic.AtomicBoolean;

public interface TaskHandler {

    void run(Task task, AtomicBoolean isRun) throws Exception;

    String getIndex();

    long getTimeoutMs();

}
