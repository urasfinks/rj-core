package ru.jamsys.core.pool;

import lombok.Getter;
import ru.jamsys.core.extension.Pollable;
import ru.jamsys.core.statistic.time.mutable.ExpirationMsMutable;
import ru.jamsys.core.statistic.time.mutable.ExpirationMsMutableImpl;

@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class PoolItem<T> extends ExpirationMsMutableImpl implements Pollable, ExpirationMsMutable {

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
