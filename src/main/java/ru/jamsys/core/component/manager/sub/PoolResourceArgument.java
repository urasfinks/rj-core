package ru.jamsys.core.component.manager.sub;

import lombok.Getter;

@Getter
public class PoolResourceArgument<T, RC> {

    private final Class<T> cls;

    private final RC resourceConstructor;

    public PoolResourceArgument(Class<T> cls, RC resourceConstructor) {
        this.cls = cls;
        this.resourceConstructor = resourceConstructor;
    }
}
