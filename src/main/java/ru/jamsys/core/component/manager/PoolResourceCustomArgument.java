package ru.jamsys.core.component.manager;

import lombok.Getter;

@Getter
public class PoolResourceCustomArgument<T, RC> {

    private final Class<T> cls;

    private final RC resourceConstructor;

    public PoolResourceCustomArgument(Class<T> cls, RC resourceConstructor) {
        this.cls = cls;
        this.resourceConstructor = resourceConstructor;
    }
}
