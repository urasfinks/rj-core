package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.annotation.PropertyName;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PropertyConnector {

    private final Map<String, Field> mapPropField = new HashMap<>();

    private final Map<String, String> mapPropValue = new HashMap<>();

    public PropertyConnector() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                String prop = field.getAnnotation(PropertyName.class).value();
                field.setAccessible(true);
                mapPropField.put(prop, field);
            }
        }
    }

    public Map<String, String> getMapPropValue(){
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

    public void setValueByProp(String prop, String value) {
        try {
            if (mapPropField.containsKey(prop)) {
                mapPropField.get(prop).set(this, value);
            }
        } catch (Exception e) {
            App.error(e);
        }
    }

}
