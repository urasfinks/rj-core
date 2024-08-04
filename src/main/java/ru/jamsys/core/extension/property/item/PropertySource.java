package ru.jamsys.core.extension.property.item;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;
import ru.jamsys.core.flat.util.Util;

import java.util.LinkedHashSet;
import java.util.Set;

// Элемент ServiceProperty, хранит в себе ссылки на подписчиков, кто наблюдает за изменением значений

public class PropertySource {

    @Setter
    @Getter
    private boolean isUpdateValue = false;

    private final String prop;

    @Getter
    private String value;

    public PropertySource(@NonNull String prop) {
        this.prop = prop;
    }

    @Getter
    private final Set<PropertySubscriber> subscribers = new LinkedHashSet<>();

    public void add(PropertySubscriber propertySubscriber) {
        this.subscribers.add(propertySubscriber);
    }

    public void remove(PropertySubscriber propertySubscriber) {
        this.subscribers.remove(propertySubscriber);
    }

    public void check(PropertySubscriber propertySubscriber) {
        String key = propertySubscriber.getKey();
        if (this.prop.equals(key)) {
            add(propertySubscriber);
        }
        String pattern = propertySubscriber.getPattern();
        if (pattern != null && Util.regexpFind(this.prop, pattern) != null) {
            add(propertySubscriber);
        }
    }

    public void setValue(String value) {
        if (value == null && this.value != null) {
            this.value = null;
            setUpdateValue(true);
        } else if (value == null) {
            setUpdateValue(false);
        } else if (!value.equals(this.value)) {
            this.value = value;
            setUpdateValue(true);
        } else {
            setUpdateValue(false);
        }
    }

}
