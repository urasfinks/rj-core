package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.rate.limit.RateLimit;

@Component
public class RateLimitManager extends AbstractManager<RateLimit> {

    public RateLimitManager() {
        setCleanableMap(false);
    }

    public RateLimit get(String index) {
        return getManagerElement(index, Void.class);
    }

    @Override
    public RateLimit build(String index, Class<?> classItem) {
        return new RateLimit(index);
    }

}
