package ru.jamsys.core.extension.property;

import ru.jamsys.core.extension.LifeCycleInterface;

public class PropertyDummy<T> implements LifeCycleInterface {

    private T value;

    public PropertyDummy(T value) {
        this.value = value;
    }

    public void set(T value) {
        this.value = value;

    }

    public T get() {
        return this.value;
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {

    }

}
