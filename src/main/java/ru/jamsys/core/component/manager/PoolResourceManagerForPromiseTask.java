package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.component.manager.sub.PoolResourceCustomArgument;
import ru.jamsys.core.extension.Completable;
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
        PI extends Completable & ExpirationMsMutable & Resource<RC, RA, RR>
        > extends AbstractManager<PoolResourceForPromiseTask<?, ?, ?, ?>, PoolResourceCustomArgument<PI, RC>> {

    public ManagerElement<
                PoolResourceForPromiseTask<RC, RA, RR, PI>,
                PoolResourceCustomArgument<PI, RC>
                > get(String index, PoolResourceCustomArgument<PI, RC> argument) {
        return new ManagerElement<>(index, argument.getCls(), this, argument);
    }

    @Override
    public PoolResourceForPromiseTask<?, ?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolResourceCustomArgument<PI, RC> customArgument
    ) {
        PoolResourceForPromiseTask<RC, RA, RR, PI> poolResourceForPromiseTask = new PoolResourceForPromiseTask<>(index, customArgument);
        poolResourceForPromiseTask.run();
        return poolResourceForPromiseTask;
    }

}
