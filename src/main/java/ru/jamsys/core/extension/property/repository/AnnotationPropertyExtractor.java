package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import lombok.ToString;
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
@Getter
public class AnnotationPropertyExtractor implements PropertyRepository {

    @Getter
    @ToString
    public static class FieldData {

        @ToString.Exclude
        private final Field field;
        private final String variableName;
        private final String propertyName;
        private final String description;
        private final boolean notNull;

        public FieldData(Field field, String variableName, String propertyName, String description, boolean notNull) {
            this.field = field;
            this.variableName = variableName;
            this.propertyName = propertyName;
            this.description = description;
            this.notNull = notNull;
        }

    }

    private final Map<Field, FieldData> fields = new HashMap<>();

    public AnnotationPropertyExtractor() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PropertyName.class)) {
                // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                field.setAccessible(true);
                fields.put(field, new FieldData(
                        field,
                        field.getName(),
                        field.isAnnotationPresent(PropertyName.class)
                                ? field.getAnnotation(PropertyName.class).value()
                                : null,
                        field.isAnnotationPresent(PropertyDescription.class)
                                ? field.getAnnotation(PropertyDescription.class).value()
                                : null,
                        field.isAnnotationPresent(PropertyNotNull.class)
                ));
            }
        }
    }

    public FieldData getFieldDataByVariableName(String FieldNameConstants) { //@FieldNameConstants -> FileByteProperty.Fields.folder
        for (Field field : fields.keySet()) {
            FieldData fieldData = fields.get(field);
            if (fieldData.getVariableName().equals(FieldNameConstants)) {
                return fieldData;
            }
        }
        return null;
    }

    public FieldData getFieldDataByPropertyName(String propertyName) { //@FieldNameConstants -> FileByteProperty.Fields.folder
        for (Field field : fields.keySet()) {
            FieldData fieldData = fields.get(field);
            if (fieldData.getPropertyName().equals(propertyName)) {
                return fieldData;
            }
        }
        return null;
    }

    // Свойства в конструкторе ещё не инициализированы field.get(this) = null
    // Заполнение propertyReferences выполняется позже
    // Вызывается в PropertySubscriber для того, что бы создать подписки и получать уведомления об изменениях для
    // PropertyUpdater
    public Map<String, String> getRepository() { //key: key: value: defValue
        Map<String, String> result = new LinkedHashMap<>();
        fields.forEach((field, fieldData) -> {
            try {
                Object fieldValue = field.get(this);
                result.put(fieldData.getPropertyName(), fieldValue == null ? null : String.valueOf(fieldValue));
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        });
        return result;
    }

    @Override
    public void setRepository(String propertyName, String value) {
        FieldData fieldData = getFieldDataByPropertyName(propertyName);
        if (fieldData == null) {
            throw new RuntimeException(getClass().getName()
                    + " fields.get(" + propertyName + ") is null; available property: "
                    + UtilJson.toStringPretty(getRepository(), "{}")
            );
        }
        if (value == null && fieldData.isNotNull()) {
            throw new RuntimeException(getClass().getName() + " propertyName: " + propertyName + "; set null value");
        }
        try {
            fieldData.getField().set(
                    this,
                    PropertyUtil.convertType.get(fieldData.getField().getType()).apply(value)
            );
        } catch (Throwable th) {
            throw new ForwardException("setRepository(" + propertyName + ", " + value + "); field: " + fieldData, th);
        }
    }

    @Override
    public PropertyRepository checkNotNull() {
        UtilRisc.forEach(null, fields, (field, fieldData) -> {
            if (fieldData.isNotNull()) {
                try {
                    Object fieldValue = field.get(this);
                    if (fieldValue == null) {
                        throw new RuntimeException(
                                getClass().getName()
                                        + " propertyName: " + fieldData.getPropertyName()
                                        + " variableName: " + fieldData.getVariableName()
                                        + "; value is null");
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return this;
    }

    @Override
    public String getDescription(String propertyName) {
        FieldData fieldData = getFieldDataByPropertyName(propertyName);
        if (fieldData != null) {
            return fieldData.getDescription();
        }
        return null;
    }

}
