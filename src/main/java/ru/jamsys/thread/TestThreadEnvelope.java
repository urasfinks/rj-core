package ru.jamsys.thread;

import ru.jamsys.pool.ThreadPool;

public class TestThreadEnvelope {
    public static ThreadEnvelope get() {
        ThreadPool threadPool = new ThreadPool(
                "Test",
                1,
                (ThreadEnvelope threadEnvelope) -> false
        );
        threadPool.run();
        return threadPool.getPoolItem();
    }
}
