package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.resource.PoolPromiseTaskWaitResource;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class ManagerPoolPromiseTaskWaitResource<
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RA, RR>
        > extends AbstractManager<PoolPromiseTaskWaitResource<?, ?, ?>, PoolSettings<PI>>
        implements KeepAliveComponent {

    public PoolPromiseTaskWaitResource<RA, RR, PI> get(String index, PoolSettings<PI> argument) {
        return (PoolPromiseTaskWaitResource) getManagerElement(index, argument.getClassPoolItem(), argument);
    }

    @Override
    public PoolPromiseTaskWaitResource<?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolSettings<PI> poolSettings
    ) {
        PoolPromiseTaskWaitResource<RA, RR, ?> poolPromiseTaskWaitResource = new PoolPromiseTaskWaitResource<>(
                index,
                poolSettings,
                (Class<PI>) classItem
        );
        poolPromiseTaskWaitResource.run();
        return poolPromiseTaskWaitResource;
    }

    @Override
    public int getInitializationIndex() {
        return 500;
    }

}
