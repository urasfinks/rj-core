package ru.jamsys.core.component.manager.sub;

import lombok.Getter;
import ru.jamsys.core.resource.Resource;

import java.util.function.Function;

@Getter
public class PoolSettings<T extends Resource<?, ?, ?>, RC> {

    private final Class<T> classPoolItem;

    private final RC resourceConstructor;

    private final Function<Throwable, Boolean> isFatalExceptionOnComplete;

    private final String index;

    public PoolSettings(
            String index,
            Class<T> classPoolItem,
            RC resourceConstructor,
            Function<Throwable, Boolean> isFatalExceptionOnComplete
    ) {
        this.index = index;
        this.classPoolItem = classPoolItem;
        this.resourceConstructor = resourceConstructor;
        this.isFatalExceptionOnComplete = isFatalExceptionOnComplete;
    }
}
