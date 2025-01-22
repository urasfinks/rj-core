package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.resource.Resource;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

public class PoolItemEnvelope<RA, RR, PI extends Resource<RA, RR>> implements AutoCloseable {

    final Pool<RA, RR, PI> pool;

    @Getter
    final PI item;

    @Setter
    private Throwable throwable = null;

    public PoolItemEnvelope(Pool<RA, RR, PI> pool, PI item) {
        this.pool = pool;
        this.item = item;
    }

    @Override
    public void close() throws Exception {
        this.pool.completePoolItem(this.item, throwable);
    }

}
