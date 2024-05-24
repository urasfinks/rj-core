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
        RA,
        RR,
        PI extends Completable & ExpirationMsMutable & Resource<RA, RR>
        > extends AbstractManager<PoolResource<?, ?, ?>, Class<PI>> {

    public ManagerElement<PoolResource<RA, RR, PI>, Class<PI>> get(String index, Class<PI> classItem) {
        return new ManagerElement<>(index, classItem, this, classItem);
    }

    @Override
    public PoolResource<?, ?, ?> build(String index, Class<?> classItem, Class<PI> customArgument) {
        PoolResource<RA, RR, PI> poolResource = new PoolResource<>(index, customArgument);
        poolResource.run();
        return poolResource;
    }

}
