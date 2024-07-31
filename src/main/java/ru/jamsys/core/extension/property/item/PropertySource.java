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
    private final Set<PropertyFollower> follower = new LinkedHashSet<>();

    public void add(PropertyFollower follower) {
        this.follower.add(follower);
    }

    public void remove(PropertyFollower follower) {
        this.follower.remove(follower);
    }

    public void check(PropertyFollower propertyFollower) {
        String key = propertyFollower.getKey();
        if (this.prop.equals(key)) {
            add(propertyFollower);
        }
        String pattern = propertyFollower.getPattern();
        if (pattern != null && Util.regexpFind(this.prop, pattern) != null) {
            add(propertyFollower);
        }
    }

    public void setValue(String value) {
        if (!value.equals(this.value)) {
            this.value = value;
            setUpdateValue(true);
        } else {
            setUpdateValue(false);
        }
    }

}
