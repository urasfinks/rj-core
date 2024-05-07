package ru.jamsys.core.pool;

import lombok.Getter;
import ru.jamsys.core.extension.Pollable;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutable;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;

@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class PoolItem<T> extends ExpiredMsMutableImpl implements Pollable, ExpiredMsMutable {

    protected final Pool<T> pool;

    protected PoolItem(Pool<T> pool) {
        this.pool = pool;
    }

    public void closeAndRemove() {
        @SuppressWarnings("unchecked")
        T poolItem = (T) this;
        pool.removeAndClose(poolItem);
    }

}
