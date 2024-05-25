package ru.jamsys.core.component.manager;

import lombok.Getter;

@Getter
public class PoolResourceCustomArgument<T, RC> {

    private Class<T> cls;

    private RC resourceConstructor;

    public PoolResourceCustomArgument(Class<T> cls, RC resourceConstructor) {
        this.cls = cls;
        this.resourceConstructor = resourceConstructor;
    }
}
