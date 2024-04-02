package ru.jamsys.thread;

import ru.jamsys.pool.Pool;

import java.util.function.Function;

public class TestThreadEnvelope extends ThreadEnvelope {
    public TestThreadEnvelope(String name, Pool<ThreadEnvelope> pool, Function<ThreadEnvelope, Boolean> consumer) {
        super(name, pool, consumer);
        isRun.set(true);
    }
}
