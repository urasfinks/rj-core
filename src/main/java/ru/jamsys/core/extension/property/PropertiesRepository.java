package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.annotation.PropertyName;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Класс помогает создать 2 map на основе свойств класса родителя помеченных аннотацией PropertyName,
// Используется как подготовленный кеш на доступ к полям, значение которых при обновлении property нужно изменить
// Можно использовать как виртуальный кеш без свойств класса

@Getter
public class PropertiesRepository {

    private final Map<String, Field> mapPropField = new HashMap<>();

    private final Map<String, String> mapPropValue = new LinkedHashMap<>();

    // Репозиторий на основе полей класса
    public PropertiesRepository() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                String prop = field.getAnnotation(PropertyName.class).value();
                field.setAccessible(true);
                mapPropField.put(prop, field);
            }
        }
    }

    // Виртуальный репозиторий
    public PropertiesRepository(Map<String, String> mapPropValue) {
        if (mapPropValue != null) {
            this.mapPropValue.putAll(mapPropValue);
        }
    }

    public void addProp(String key, String value) {
        this.mapPropValue.computeIfAbsent(key, _ -> value);
    }

    public Map<String, String> getMapPropValue(){
        if (mapPropValue.isEmpty()) {
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
        }
        return mapPropValue;
    }

    public void setValueByProp(String prop, String value) {
        try {
            mapPropValue.put(prop, value);
            if (mapPropField.containsKey(prop)) {
                mapPropField.get(prop).set(this, value);
            }
        } catch (Exception e) {
            App.error(e);
        }
    }

}
