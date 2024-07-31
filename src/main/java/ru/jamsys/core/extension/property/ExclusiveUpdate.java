package ru.jamsys.core.extension.property;

import java.util.function.Consumer;

public class ExclusiveUpdate<T> {

    Class<T> cls;

    Consumer<T> onUpdate;

    public ExclusiveUpdate(Class<T> cls, Consumer<T> onUpdate) {
        this.cls = cls;
        this.onUpdate = onUpdate;
    }

    public void onUpdate(String value) {
        if (this.onUpdate != null) {
            @SuppressWarnings("unchecked")
            T val = (T) Property.convertType.get(cls).apply(value);
            this.onUpdate.accept(val);
        }
    }

}
