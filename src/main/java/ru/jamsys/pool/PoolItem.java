package ru.jamsys.pool;

import ru.jamsys.extension.Pollable;
import ru.jamsys.statistic.TimeController;
import ru.jamsys.statistic.TimeControllerImpl;

public abstract class PoolItem<T> extends TimeControllerImpl implements Pollable, TimeController {

    protected final Pool<T> pool;

    protected PoolItem(Pool<T> pool) {
        this.pool = pool;
    }

    public Pool<T> getPool() {
        return pool;
    }

    public void closeAndRemove() {
        @SuppressWarnings("unchecked")
        T poolItem = (T) this;
        pool.removeAndClose(poolItem);
    }

}
