package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.resource.PoolResourceForPromiseTask;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RC - ResourceConstructor
// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class PoolResourceManagerForPromiseTask<
        RC,
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RC, RA, RR>
        > extends AbstractManager<PoolResourceForPromiseTask<?, ?, ?, ?>, PoolSettings<PI, RC>>
        implements KeepAliveComponent {

    public PoolResourceForPromiseTask<RC, RA, RR, PI> get(String index, PoolSettings<PI, RC> argument) {
        return (PoolResourceForPromiseTask) getManagerElement(index, argument.getClassPoolItem(), argument);
    }

    @Override
    public PoolResourceForPromiseTask<?, ?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolSettings<PI, RC> builderArgument
    ) {
        PoolResourceForPromiseTask<RC, RA, RR, ?> poolResourceForPromiseTask = new PoolResourceForPromiseTask<>(
                index,
                builderArgument,
                (Class<PI>) classItem
        );
        poolResourceForPromiseTask.run();
        return poolResourceForPromiseTask;
    }

    @Override
    public int getInitializationIndex() {
        return 997;
    }

}
