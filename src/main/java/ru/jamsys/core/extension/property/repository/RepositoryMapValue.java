package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import ru.jamsys.core.extension.property.Property;

@Getter
public class RepositoryMapValue<T> {

    Class<T> cls;

    T value;

    public RepositoryMapValue(Class<T> cls, T value) {
        this.cls = cls;
        this.value = value;
    }

    public RepositoryMapValue(Class<T> cls, String value) {
        this.cls = cls;
        setValue(value);
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = null;
            return;
        }
        @SuppressWarnings("unchecked")
        T t = (T) Property.convertType.get(cls).apply(value);
        this.value = t;
    }

}
