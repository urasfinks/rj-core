package ru.jamsys.core.extension.property.item;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.property.PropertyNs;

import java.util.function.Consumer;

@Getter
public class SubscriberItem<T> {

    private final T defValue;

    private final boolean require;

    @Setter
    private boolean isSubscribe = false;

    private final Consumer<T> onUpdate;

    private final Class<T> cls;

    public SubscriberItem(Class<T> cls, T defValue, boolean require, Consumer<T> onUpdate) {
        this.cls = cls;
        this.defValue = defValue;
        this.require = require;
        this.onUpdate = onUpdate;
    }

    public void onUpdate(String value) {
        if (this.onUpdate != null) {
            @SuppressWarnings("unchecked")
            T val = (T) PropertyNs.convertType.get(cls).apply(value);
            this.onUpdate.accept(val);
        }
    }

}
