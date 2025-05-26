package ru.jamsys.core.extension;

import ru.jamsys.core.pool.Valid;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class HotSwap<T extends Valid> {

    private volatile T resource;

    private final Supplier<T> builder;

    private final ReentrantLock lock = new ReentrantLock();

    public HotSwap(Supplier<T> builder) {
        this.builder = builder;
        this.resource = builder.get();
    }

    public T get() {
        if (!resource.isValid()) {
            try {
                lock.lock();
                if (!resource.isValid()) {
                    resource = builder.get();
                }
            } finally {
                lock.unlock();
            }
        }
        return resource;
    }

}
