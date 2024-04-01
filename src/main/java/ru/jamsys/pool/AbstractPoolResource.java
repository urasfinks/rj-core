package ru.jamsys.pool;

import ru.jamsys.extension.Pollable;
import ru.jamsys.statistic.AbstractTimeController;
import ru.jamsys.statistic.TimeController;

public abstract class AbstractPoolResource<T extends TimeController> extends AbstractTimeController implements Pollable {

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
