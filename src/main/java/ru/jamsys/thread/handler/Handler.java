package ru.jamsys.thread.handler;

import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.Task;

public interface Handler<T extends Task> {

    void run(T task, ThreadEnvelope threadEnvelope) throws Exception;

}
