package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.resource.Resource;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

public class PoolItemEnvelope<RC, RA, RR, PI extends Resource<RC, RA, RR>> implements AutoCloseable {

    final Pool<RC, RA, RR, PI> pool;

    @Getter
    final PI item;

    @Setter
    private Exception e = null;

    public PoolItemEnvelope(Pool<RC, RA, RR, PI> pool, PI item) {
        this.pool = pool;
        this.item = item;
    }

    @Override
    public void close() throws Exception {
        this.pool.complete(this.item, e);
    }

}