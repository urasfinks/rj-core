package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.repository.AbstractRepositoryProperty;
import ru.jamsys.core.flat.util.UtilJson;

import java.lang.reflect.Field;
import java.util.Objects;

// Это обёртка для оригинального Property, что бы хранить в репозитории.
// Содержит ключ репозитория, абсолютный ключ Property, ссылку на связанное поле репозитория, возможность notNull
// и описание.

@Getter
@Accessors(chain = true)
@JsonPropertyOrder({"propertyKey", "repositoryPropertyKey", "fieldNameConstants", "cls", "description", "notNull", "dynamic", "value"})
public class PropertyEnvelope<T> {

    // Когда свойства в репозиторий добавляются по regexp
    @Setter
    private boolean dynamic = false;

    private final Class<T> cls;

    @JsonIgnore
    private Field field;

    // Имя переменной в наследнике AnnotationPropertyExtractor
    private final String fieldNameConstants;
    // Значение аннотации @PropertyKey для переменной наследника AnnotationPropertyExtractor
    private final String repositoryPropertyKey;

    @Setter
    private String description;
    private final boolean notNull;
    private T value;

    //propertyKey - проставляется автоматически при создании подписки
    @Setter
    private String propertyKey;

    @JsonIgnore
    @Setter
    ServiceProperty serviceProperty;

    @JsonIgnore
    @Setter
    AbstractRepositoryProperty<T> repositoryProperty;

    public PropertyEnvelope(
            Field field,
            Class<T> cls,
            String fieldNameConstants,
            String repositoryPropertyKey,
            T value,
            String description,
            boolean notNull
    ) {
        this.field = field;
        this.cls = cls;
        this.fieldNameConstants = fieldNameConstants;
        this.repositoryPropertyKey = repositoryPropertyKey;
        this.description = description;
        this.notNull = notNull;
        this.value = value;
    }

    public PropertyEnvelope(
            Class<T> cls,
            String repositoryPropertyKey,
            T value,
            boolean notNull
    ) {
        this.cls = cls;
        this.repositoryPropertyKey = repositoryPropertyKey;
        this.fieldNameConstants = null;
        this.notNull = notNull;
        this.value = value;
        this.description = null;
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
        return serviceProperty.equals(getPropertyKey(), getValueString());
    }

    // Пролить значения до PropertyRepository
    public PropertyEnvelope<T> syncPropertyValue() {
        // Получили default значение, получили Property, если не сошлись, считаем приоритетным Property.get()
        Property property = serviceProperty.computeIfAbsent(
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
        try {
            @SuppressWarnings("unchecked")
            T apply = (T) PropertyUtil.convertType.get(cls).apply(property.get());
            this.value = apply;
        } catch (Throwable th) {
            //"PropertyUtil.convertType.get(" + cls + ").apply(" + property.get() + ");"
            throw new ForwardException(UtilJson.toStringPretty(this, "{}"), th);
        }
        this.description = property.getDescription();
        return this;
    }

}
