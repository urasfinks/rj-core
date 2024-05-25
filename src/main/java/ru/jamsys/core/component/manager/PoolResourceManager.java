package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.resource.PoolResource;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RC - ResourceConstructor
// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class PoolResourceManager<
        RC,
        RA,
        RR,
        PI extends Completable & ExpirationMsMutable & Resource<RC, RA, RR>
        > extends AbstractManager<PoolResource<?, ?, ?, ?>, PoolResourceCustomArgument<PI, RC>> {

    public ManagerElement<
            PoolResource<RC, RA, RR, PI>,
            PoolResourceCustomArgument<PI, RC>
            > get(String index, PoolResourceCustomArgument<PI, RC> argument) {
        return new ManagerElement<>(index, argument.getCls(), this, argument);
    }

    @Override
    public PoolResource<?, ?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolResourceCustomArgument<PI, RC> customArgument
    ) {
        PoolResource<RC, RA, RR, PI> poolResource = new PoolResource<>(index, customArgument);
        poolResource.run();
        return poolResource;
    }

}
