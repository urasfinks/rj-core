package ru.jamsys.thread.handler;

import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.AbstractTask;

public interface Handler<T extends AbstractTask> {

    void run(T task, ThreadEnvelope threadEnvelope) throws Exception;

}
