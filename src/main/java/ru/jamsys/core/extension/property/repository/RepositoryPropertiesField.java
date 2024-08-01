package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.annotation.PropertyName;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Класс помогает изъять аннтоции свойств

public class RepositoryPropertiesField implements RepositoryProperties {

    private final Map<String, Field> mapProperties = new HashMap<>();

    public RepositoryPropertiesField() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                String prop = field.getAnnotation(PropertyName.class).value();
                field.setAccessible(true);
                mapProperties.put(prop, field);
            }
        }
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> mapPropValue = new LinkedHashMap<>();
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                String prop = field.getAnnotation(PropertyName.class).value();
                field.setAccessible(true);
                try {
                    mapPropValue.put(prop, (String) field.get(this));
                } catch (Exception e) {
                    App.error(e);
                }
            }
        }
        return mapPropValue;
    }

    @Override
    public void setProperty(String prop, String value) {
        try {
            if (mapProperties.containsKey(prop)) {
                mapProperties.get(prop).set(this, value);
            }
        } catch (Exception e) {
            App.error(e);
        }
    }

}
