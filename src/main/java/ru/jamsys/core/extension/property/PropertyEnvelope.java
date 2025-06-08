package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.repository.AbstractRepositoryProperty;
import ru.jamsys.core.flat.util.UtilJson;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.regex.Pattern;

// Это обёртка для оригинального Property, что бы хранить в репозитории.
// Содержит ключ репозитория, абсолютный ключ Property, ссылку на связанное поле репозитория, возможность notNull
// и описание.

@Getter
@Accessors(chain = true)
public class PropertyEnvelope<T> {

    private final boolean dynamic; // Когда свойства в репозиторий добавляются по regexp

    private final Class<T> cls;

    private final Field field;

    // Имя переменной в наследнике AnnotationPropertyExtractor
    private final String fieldNameConstants;

    // Значение аннотации @PropertyKey для переменной наследника AnnotationPropertyExtractor
    private final String repositoryPropertyKey;

    private String description;

    private final boolean notNull;

    private T value;

    //propertyKey - проставляется автоматически при создании подписки
    @Setter
    private String propertyKey;

    @JsonIgnore
    private final AbstractRepositoryProperty<T> repositoryProperty;

    private final Pattern regexp;

    public PropertyEnvelope(
            AbstractRepositoryProperty<T> repositoryProperty,
            Field field,
            Class<T> cls,
            String fieldNameConstants,
            String repositoryPropertyKey,
            T value,
            String description,
            String regexp,
            boolean notNull,
            boolean dynamic
    ) {
        this.repositoryProperty = repositoryProperty;
        this.field = field;
        this.cls = cls;
        this.fieldNameConstants = fieldNameConstants;
        this.repositoryPropertyKey = repositoryPropertyKey;
        this.description = description;
        this.notNull = notNull;
        this.value = value;
        this.regexp = regexp == null ? null : Pattern.compile(regexp);
        this.dynamic = dynamic;
    }

    @JsonValue
    public HashMapBuilder<String, Object> getJsonValue(){
        return new HashMapBuilder<String, Object>()
                .append("propertyKey", propertyKey)
                .append("repositoryPropertyKey", repositoryPropertyKey)
                .append("fieldNameConstants", fieldNameConstants)
                .append("cls", cls)
                .append("description", description)
                .append("notNull", notNull)
                .append("dynamic", dynamic)
                .append("regexp", regexp)
                .append("value", value)
                ;
    }

    @Override
    public String toString() {
        return UtilJson.toString(this, "--");
    }

    @JsonIgnore
    public String getValueString() {
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyEnvelope<?> that = (PropertyEnvelope<?>) o;
        // У репы может быть несколько полей ссылающихся на 1 ключ property, поэтому ключ репозитория должен быть уникальным
        return Objects.equals(repositoryPropertyKey, that.repositoryPropertyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(repositoryPropertyKey);
    }

    public ServiceProperty.Equals propertyEquals() {
        return App.get(ServiceProperty.class).equals(getPropertyKey(), getValueString());
    }

    // Пролить значения до PropertyRepository
    public PropertyEnvelope<T> syncPropertyValue() {
        // Получили default значение, получили Property, если не сошлись, считаем приоритетным Property.get()
        Property property = App.get(ServiceProperty.class).computeIfAbsent(
                getPropertyKey(),
                getValueString(),
                property1 -> {
                    if (repositoryProperty != null) {
                        property1.getTraceSetup().getLast().setResource(repositoryProperty.getClass().getName());
                    }
                    if (description != null) {
                        property1.setDescriptionIfNull(description);
                    }
                });
        String propertyValue = property.get();
        if (regexp != null && !regexp.matcher(propertyValue).matches()) {
            throw new ForwardException(String.format(
                    "Validation failed: value '%s' does not match expected pattern '%s'",
                    propertyValue,
                    regexp.pattern()
            ), this);
        }
        try {
            @SuppressWarnings("unchecked")
            T apply = (T) PropertyUtil.convertType.get(cls).apply(propertyValue);

            this.value = apply;
        } catch (Throwable th) {
            throw new ForwardException(this, th);
        }
        this.description = property.getDescription();
        return this;
    }

    public PropertyEnvelope<T> apply() {
        try {
            field.set(repositoryProperty, getValue());
        } catch (Throwable th) {
            throw new ForwardException(this, th);
        }
        return this;
    }

}
