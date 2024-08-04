package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.Property;

@Getter
public class RepositoryMapValue<T> {

    private final Class<T> cls;

    private T value;

    private final String prop;

    public RepositoryMapValue(Class<T> cls, T value, String prop) {
        this.cls = cls;
        this.value = value;
        this.prop = prop;
    }

    public RepositoryMapValue(Class<T> cls, String value, String prop) {
        this.cls = cls;
        this.prop = prop;
        setValue(value);
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = null;
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            T t = (T) Property.convertType.get(cls).apply(value);
            this.value = t;
        } catch (Throwable th) {
            throw new ForwardException("RepositoryMapValue.setValue('" + value + "') for prop: " + prop, th);
        }
    }

}
