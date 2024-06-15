package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.resource.PoolTask;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RC - ResourceConstructor
// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class ManagerPoolTask<
        RC,
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RC, RA, RR>
        > extends AbstractManager<PoolTask<?, ?, ?, ?>, PoolSettings<PI, RC>>
        implements KeepAliveComponent {

    public PoolTask<RC, RA, RR, PI> get(String index, PoolSettings<PI, RC> argument) {
        return (PoolTask) getManagerElement(index, argument.getClassPoolItem(), argument);
    }

    @Override
    public PoolTask<?, ?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolSettings<PI, RC> poolSettings
    ) {
        PoolTask<RC, RA, RR, ?> poolTask = new PoolTask<>(
                index,
                poolSettings,
                (Class<PI>) classItem
        );
        poolTask.run();
        return poolTask;
    }

    @Override
    public int getInitializationIndex() {
        return 997;
    }

}
