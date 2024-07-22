package ru.jamsys.core.extension.property.item;

import ru.jamsys.core.component.ServiceClassFinder;

public abstract class PropertyInstance<T> implements PropertyConverter {

    protected T value;

    public PropertyInstance(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public static <X> PropertyInstance<X> instanceOf(Class<X> cls, X value) {
        PropertyInstance<?> resultInstance = null;
        if (ServiceClassFinder.instanceOf(cls, String.class)) {
            resultInstance = new PropertyString((String) value);
        } else if (ServiceClassFinder.instanceOf(cls, Boolean.class)) {
            resultInstance = new PropertyBoolean((Boolean) value);
        } else if (ServiceClassFinder.instanceOf(cls, Integer.class)) {
            resultInstance = new PropertyInteger((Integer) value);
        }
        return (PropertyInstance<X>) resultInstance;
    }

}
