package ru.jamsys.core.component.manager.sub;

import lombok.Getter;

import java.util.function.Function;

@Getter
public class PoolResourceArgument<T, RC> {

    private final Class<T> classPoolItem;

    private final RC resourceConstructor;

    private final Function<Exception, Boolean> checkExceptionOnComplete;

    public PoolResourceArgument(
            Class<T> classPoolItem,
            RC resourceConstructor,
            Function<Exception, Boolean> checkExceptionOnComplete
    ) {
        this.classPoolItem = classPoolItem;
        this.resourceConstructor = resourceConstructor;
        this.checkExceptionOnComplete = checkExceptionOnComplete;
    }
}
