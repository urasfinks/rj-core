package ru.jamsys.core.extension.property.item.type;

public abstract class PropertyInstance<T> implements PropertyConverter {

    protected T value;

    public PropertyInstance(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

}