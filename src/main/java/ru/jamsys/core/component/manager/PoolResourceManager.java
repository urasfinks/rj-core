package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.resource.PoolResource;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class PoolResourceManager<
        RC,
        RA,
        RR,
        PI extends Completable & ExpirationMsMutable & Resource<RC, RA, RR>
        > extends AbstractManager<PoolResource<?, ?, ?, ?>, Class<PI>> {

    public ManagerElement<PoolResource<RC, RA, RR, PI>, Class<PI>> get(String index, Class<PI> classItem) {
        return new ManagerElement<>(index, classItem, this, classItem);
    }

    @Override
    public PoolResource<?, ?, ?, ?> build(String index, Class<?> classItem, Class<PI> customArgument) {
        PoolResource<RC, RA, RR, PI> poolResource = new PoolResource<>(index, customArgument);
        poolResource.run();
        return poolResource;
    }

}
