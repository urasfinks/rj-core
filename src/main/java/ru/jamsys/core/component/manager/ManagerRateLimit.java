package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.rate.limit.RateLimit;

@Component
public class ManagerRateLimit extends AbstractManager<RateLimit, Void> implements CascadeName {

    public ManagerRateLimit() {
        setCleanableMap(false);
    }

    public RateLimit get(String key) {
        return getManagerElement(key, Void.class, null);
    }

    @Override
    public RateLimit build(String key, Class<?> classItem, Void builderArgument) {
        return new RateLimit(this, key);
    }

    @Override
    public int getInitializationIndex() {
        return 2;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }

}
