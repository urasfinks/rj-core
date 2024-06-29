package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.rate.limit.RateLimit;

@Component
public class ManagerRateLimit extends AbstractManager<RateLimit, Void> {

    public ManagerRateLimit() {
        setCleanableMap(false);
    }

    public RateLimit get(String index) {
        return getManagerElement(index, Void.class, null);
    }

    @Override
    public RateLimit build(String index, Class<?> classItem, Void builderArgument) {
        return new RateLimit(index);
    }

    @Override
    public int getInitializationIndex() {
        return 1;
    }

}
