package ru.jamsys.core.extension.property;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.item.PropertiesRepository;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Класс помогает изъять аннтоции свойств

public class PropertiesRepositoryField implements PropertiesRepository {

    private final Map<String, Field> mapProp = new HashMap<>();

    public PropertiesRepositoryField() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                String prop = field.getAnnotation(PropertyName.class).value();
                field.setAccessible(true);
                mapProp.put(prop, field);
            }
        }
    }

    @Override
    public Map<String, String> getPropValue() {
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
    public void setPropValue(String prop, String value) {
        try {
            if (mapProp.containsKey(prop)) {
                mapProp.get(prop).set(this, value);
            }
        } catch (Exception e) {
            App.error(e);
        }
    }

}
