package ru.jamsys.thread.task;

import lombok.Getter;
import ru.jamsys.pool.Pool;
import ru.jamsys.thread.ThreadEnvelope;

@Getter
public class RollbackThreadEnvelopeInParkTask extends Task {

    final ThreadEnvelope threadEnvelope;

    final Exception exception;

    final Pool<ThreadEnvelope> pool;

    public RollbackThreadEnvelopeInParkTask(ThreadEnvelope threadEnvelope, Exception exception, Pool<ThreadEnvelope> pool) {
        this.threadEnvelope = threadEnvelope;
        this.exception = exception;
        this.pool = pool;
    }
}
