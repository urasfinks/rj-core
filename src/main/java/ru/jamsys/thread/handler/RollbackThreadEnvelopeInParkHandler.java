package ru.jamsys.thread.handler;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.RollbackThreadEnvelopeInParkTask;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class RollbackThreadEnvelopeInParkHandler implements Handler<RollbackThreadEnvelopeInParkTask> {

    @Override
    public void run(RollbackThreadEnvelopeInParkTask task, AtomicBoolean isRun) throws Exception {
        task.getPool().complete(task.getThreadEnvelope(), task.getException());
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }

}
