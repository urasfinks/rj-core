package ru.jamsys.core.extension.property.item;

public abstract class PropertyConverter<T> implements iface {

    protected T value;

    public PropertyConverter(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

}
