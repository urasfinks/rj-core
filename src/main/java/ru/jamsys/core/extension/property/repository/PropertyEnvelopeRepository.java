package ru.jamsys.core.extension.property.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertyUtil;
import ru.jamsys.core.flat.util.UtilJson;

import java.lang.reflect.Field;
import java.util.Objects;

// Это Property обёртка, что бы удобно хранить в репозитории.
// Содержит ключ репозитория, абсолютный ключ Property, ссылку на связанное поле репозитория, возможность notNull
// и описание

@Getter
@Accessors(chain = true)
@JsonPropertyOrder({"propertyKey", "repositoryPropertyKey", "fieldNameConstants", "cls", "description", "notNull", "dynamic", "value"})
public class PropertyEnvelopeRepository<T> {

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
    PropertyRepository<T> propertyRepository;

    public PropertyEnvelopeRepository(
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

    public PropertyEnvelopeRepository(
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
        PropertyEnvelopeRepository<?> that = (PropertyEnvelopeRepository<?>) o;
        // У репы может быть несколько полей ссылающихся на 1 ключ property, поэтому ключ репозитория должен быть уникальным
        return Objects.equals(repositoryPropertyKey, that.repositoryPropertyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(repositoryPropertyKey);
    }

//    @SuppressWarnings("unused") // UtilJson
//    public int getId() {
//        return super.hashCode();
//    }

    public ServiceProperty.Equals propertyEquals() {
        return serviceProperty.equals(getPropertyKey(), getValueString());
    }

    // Пролить значения до PropertyRepository
    // Если
    public PropertyEnvelopeRepository<T> syncPropertyValue() {
        // Получили default значение, получили Property, если не сошлись, считаем приоритетным Property.get()
        Property property = serviceProperty.computeIfAbsent(
                getPropertyKey(),
                getValueString(),
                property1 -> {
                    if (propertyRepository != null) {
                        property1.getSetTrace().getLast().setResource(propertyRepository.getClass().getName());
                    }
                    if (description != null) {
                        property1.setDescriptionIfNull(description);
                    }
                });
        @SuppressWarnings("unchecked")
        T apply = (T) PropertyUtil.convertType.get(cls).apply(property.get());
        this.value = apply;
        this.description = property.getDescription();
        return this;
    }

}
