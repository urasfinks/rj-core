package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.rate.limit.RateLimit;

@Component
public class RateLimitManager extends AbstractManager<RateLimit, Void> {

    public RateLimitManager() {
        setCleanableMap(false);
    }

    public RateLimit get(String index) {
        return getManagerElement(index, Void.class, null);
    }

    @Override
    public RateLimit build(String index, Class<?> classItem, Void customArgument) {
        return new RateLimit(index);
    }

}
