package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.rate.limit.RateLimit;

@Component
public class RateLimitManager extends AbstractManager<RateLimit> {

    public EnvelopManagerObject<RateLimit> get(String index) {
        return new EnvelopManagerObject<>(index, Void.class, this);
    }

    @Override
    public RateLimit build(String index, Class<?> classItem) {
        return new RateLimit(index);
    }

}
