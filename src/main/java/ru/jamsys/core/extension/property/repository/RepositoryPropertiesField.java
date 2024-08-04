package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.Util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Класс помогает изъять аннтоции свойств

public class RepositoryPropertiesField implements RepositoryProperties {

    private final Map<String, Field> mapField = new HashMap<>();

    private final Map<String, RepositoryMapValue<?>> mapRepository = new LinkedHashMap<>();

    public RepositoryPropertiesField() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                field.setAccessible(true);
                mapField.put(field.getAnnotation(PropertyName.class).value(), field);
            }
        }
    }

    // Проблема в том, что к конструкторе field.get(this) = null
    // Как будто значения свойств ещё не инициализированны
    @Override
    public Map<String, RepositoryMapValue<?>> getMapRepository() {
        mapField.forEach((s, field) -> mapRepository.computeIfAbsent(s, _ -> {
            try {
                return new RepositoryMapValue(field.getType(), field.get(this));
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        }));
        return mapRepository;
    }

    @Override
    public void setProperty(String prop, String value) {
        if (mapField.containsKey(prop)) {
            RepositoryMapValue<?> repositoryMapValue = mapRepository.get(prop);
            repositoryMapValue.setValue(value);
            try {
                mapField.get(prop).set(this, repositoryMapValue.getValue());
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        } else {
            Util.logConsole("RepositoryPropertiesField.setProperty('" + prop + "', '" + value + "') field not exist", true);
        }
    }

}
