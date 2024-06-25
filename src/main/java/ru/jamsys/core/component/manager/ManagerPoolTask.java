package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.resource.TaskWait;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// RC - ResourceConstructor
// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

@Component
public class ManagerPoolTask<
        RA,
        RR,
        PI extends ExpirationMsMutable & Resource<RA, RR>
        > extends AbstractManager<TaskWait<?, ?, ?>, PoolSettings<PI>>
        implements KeepAliveComponent {

    public TaskWait<RA, RR, PI> get(String index, PoolSettings<PI> argument) {
        return (TaskWait) getManagerElement(index, argument.getClassPoolItem(), argument);
    }

    @Override
    public TaskWait<?, ?, ?> build(
            String index,
            Class<?> classItem,
            PoolSettings<PI> poolSettings
    ) {
        TaskWait<RA, RR, ?> taskWait = new TaskWait<>(
                index,
                poolSettings,
                (Class<PI>) classItem
        );
        taskWait.run();
        return taskWait;
    }

    @Override
    public int getInitializationIndex() {
        return 997;
    }

}
