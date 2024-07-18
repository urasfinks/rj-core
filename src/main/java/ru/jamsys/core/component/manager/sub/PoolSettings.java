package ru.jamsys.core.component.manager.sub;

import lombok.Getter;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.Resource;

import java.util.function.Function;

@Getter
public class PoolSettings<T extends Resource<?, ?>> {

    private final String index;

    private final Class<T> classPoolItem;

    private final ResourceArguments resourceArguments;

    private final Function<Throwable, Boolean> functionCheckFatalException;

    public PoolSettings(
            String index,
            Class<T> classPoolItem,
            ResourceArguments resourceArguments,
            Function<Throwable, Boolean> functionCheckFatalException
    ) {
        this.index = index;
        this.classPoolItem = classPoolItem;
        this.resourceArguments = resourceArguments;
        this.functionCheckFatalException = functionCheckFatalException;
    }

}
