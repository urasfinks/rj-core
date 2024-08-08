package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.resource.PoolTaskWait;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class ManagerPoolTaskWait<
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RA, RR>
        > extends AbstractManager<PoolTaskWait<?, ?, ?>, PoolSettings<PI>>
        implements KeepAliveComponent {

    public PoolTaskWait<RA, RR, PI> get(String index, PoolSettings<PI> argument) {
        return (PoolTaskWait) getManagerElement(index, argument.getClassPoolItem(), argument);
    }

    @Override
    public PoolTaskWait<?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolSettings<PI> poolSettings
    ) {
        PoolTaskWait<RA, RR, ?> poolTaskWait = new PoolTaskWait<>(
                index,
                poolSettings,
                (Class<PI>) classItem
        );
        poolTaskWait.run();
        return poolTaskWait;
    }

    @Override
    public int getInitializationIndex() {
        return 500;
    }

}
