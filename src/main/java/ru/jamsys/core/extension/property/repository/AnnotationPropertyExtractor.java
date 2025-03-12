package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyUtil;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Класс помогает изъять свойства у класса родителя через аннтоции

public class AnnotationPropertyExtractor implements PropertyRepository {

    private final Map<String, Field> fields = new HashMap<>();

    public AnnotationPropertyExtractor() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                field.setAccessible(true);
                fields.put(field.getAnnotation(PropertyName.class).value(), field);
            }
        }
    }

    // Свойства в конструкторе ещё не инициализированы field.get(this) = null
    // Заполнение propertyReferences выполняется позже
    // Вызывается в PropertySubscriber для того, что бы создать подписки и получать уведомления об изменениях для
    // PropertyUpdater
    public Map<String, String> getRepository() { //key: key: value: defValue
        Map<String, String> result = new LinkedHashMap<>();
        fields.forEach((key, field) -> {
            try {
                Object fieldValue = field.get(this);
                result.put(key, fieldValue == null ? null : String.valueOf(fieldValue));
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        });
        return result;
    }

    @Override
    public void setRepository(String key, String value) {
        Field field = fields.get(key);
        if (field == null) {
            throw new RuntimeException(getClass().getName()
                    + " fields.get(" + key + ") is null; available property: "
                    + UtilJson.toStringPretty(getRepository(), "{}")
            );
        }
        if (value == null && field.isAnnotationPresent(PropertyNotNull.class)) {
            throw new RuntimeException(getClass().getName() + " key: " + key + "; set null value");
        }
        try {
            field.set(this, PropertyUtil.convertType.get(field.getType()).apply(value));
        } catch (Throwable th) {
            throw new ForwardException("setRepository(" + key + ", " + value + "); field: " + field, th);
        }
    }

    @Override
    public String getDescription(String key) {
        Field field = fields.get(key);
        if (field != null && field.isAnnotationPresent(PropertyDescription.class)) {
            return field.getAnnotation(PropertyDescription.class).value();
        }
        return null;
    }

    @Override
    public PropertyRepository checkNotNull() {
        UtilRisc.forEach(null, fields, (key, field) -> {
            if (field.isAnnotationPresent(PropertyNotNull.class)) {
                try {
                    Object fieldValue = field.get(this);
                    if (fieldValue == null) {
                        throw new RuntimeException(getClass().getName() + " key: " + key + "; value is null");
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        return this;
    }

}
