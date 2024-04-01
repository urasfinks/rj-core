package ru.jamsys.pool;

import ru.jamsys.extension.Pollable;
import ru.jamsys.statistic.AbstractExpired;
import ru.jamsys.statistic.Expired;

public abstract class AbstractPoolResource<T extends Expired> extends AbstractExpired implements Pollable {

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
