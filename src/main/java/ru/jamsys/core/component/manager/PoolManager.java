package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.resource.Pool;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RC - ResourceConstructor
// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class PoolManager<
        RC,
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RC, RA, RR>
        > extends AbstractManager<Pool<?, ?, ?, ?>, PoolSettings<PI, RC>>
        implements KeepAliveComponent {

    public Pool<RC, RA, RR, PI> get(String index, PoolSettings<PI, RC> argument) {
        return (Pool) getManagerElement(index, argument.getClassPoolItem(), argument);
    }

    @Override
    public Pool<?, ?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolSettings<PI, RC> poolSettings
    ) {
        Pool<RC, RA, RR, ?> pool = new Pool<>(
                index,
                poolSettings,
                (Class<PI>) classItem
        );
        pool.run();
        return pool;
    }

    @Override
    public int getInitializationIndex() {
        return 997;
    }

}
