package ru.jamsys.pool;

import ru.jamsys.extension.Pollable;
import ru.jamsys.statistic.TimeController;
import ru.jamsys.statistic.TimeControllerImpl;

public abstract class AbstractPoolResource<T> extends TimeControllerImpl implements Pollable, TimeController {

    protected final Pool<T> pool;

    protected AbstractPoolResource(Pool<T> pool) {
        this.pool = pool;
    }

    public Pool<T> getPool() {
        return pool;
    }

    public void closeAndRemove() {
        @SuppressWarnings("unchecked")
        T resource = (T) this;
        pool.removeAndClose(resource);
    }

}
