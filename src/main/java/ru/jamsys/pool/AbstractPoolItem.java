package ru.jamsys.pool;

import ru.jamsys.extension.Pollable;
import ru.jamsys.statistic.AbstractExpired;
import ru.jamsys.statistic.Expired;

public abstract class AbstractPoolItem<T extends Expired> extends AbstractExpired implements Pollable {

    protected final Pool<T> pool;

    protected AbstractPoolItem(Pool<T> pool) {
        this.pool = pool;
    }

    public Pool<T> getPool() {
        return pool;
    }

}
