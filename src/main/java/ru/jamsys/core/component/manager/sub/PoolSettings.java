package ru.jamsys.core.component.manager.sub;

import lombok.Getter;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;

import java.util.function.Function;

@Getter
public class PoolSettings<T extends Resource<?, ?>> {

    @Getter
    private final String key;

    private final Class<T> classPoolItem;

    private final ResourceArguments resourceArguments;

    private final Function<Throwable, Boolean> functionCheckFatalException;

    public PoolSettings(
            String key,
            Class<T> classPoolItem,
            ResourceArguments resourceArguments,
            Function<Throwable, Boolean> functionCheckFatalException
    ) {
        this.key = key;
        this.classPoolItem = classPoolItem;
        this.resourceArguments = resourceArguments;
        this.functionCheckFatalException = functionCheckFatalException;
    }

}
