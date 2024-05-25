package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.component.manager.sub.PoolResourceArgument;
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
        > extends AbstractManager<PoolResourceForPromiseTask<?, ?, ?, ?>, PoolResourceArgument<PI, RC>> {

    public ManagerElement<
                PoolResourceForPromiseTask<RC, RA, RR, PI>,
            PoolResourceArgument<PI, RC>
                > get(String index, PoolResourceArgument<PI, RC> argument) {
        return new ManagerElement<>(index, argument.getCls(), this, argument);
    }

    @Override
    public PoolResourceForPromiseTask<?, ?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolResourceArgument<PI, RC> builderArgument
    ) {
        PoolResourceForPromiseTask<RC, RA, RR, PI> poolResourceForPromiseTask = new PoolResourceForPromiseTask<>(index, builderArgument);
        poolResourceForPromiseTask.run();
        return poolResourceForPromiseTask;
    }

}
