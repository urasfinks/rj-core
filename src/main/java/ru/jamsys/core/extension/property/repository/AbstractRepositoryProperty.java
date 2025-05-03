package ru.jamsys.core.extension.property.repository;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonPropertyOrder({"listPropertyEnvelopeRepository", "init", "cls"})
@Getter
public abstract class AbstractRepositoryProperty<T> {

    abstract public void init(PropertyDispatcher<T> propertyDispatcher);

    private final List<PropertyEnvelope<T>> listPropertyEnvelopeRepository = new ArrayList<>();

    private final AtomicBoolean init = new AtomicBoolean(false);

    public void append(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
    }

    abstract public void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher);

    public void checkNotNull() {
        UtilRisc.forEach(null, getListPropertyEnvelopeRepository(), fieldData -> {
            if (fieldData.isNotNull() && fieldData.getValue() == null) {
                throw new RuntimeException(UtilJson.toStringPretty(this, "--"));
            }
        });
    }

    public PropertyEnvelope<T> getByFieldNameConstants(String FieldNameConstants) { //@FieldNameConstants -> FileByteProperty.Fields.folder
        for (PropertyEnvelope<T> propertyEnvelope : getListPropertyEnvelopeRepository()) {
            if (propertyEnvelope.getFieldNameConstants().equals(FieldNameConstants)) {
                return propertyEnvelope;
            }
        }
        return null;
    }

    public PropertyEnvelope<T> getByRepositoryPropertyKey(String repositoryPropertyKey) {
        for (PropertyEnvelope<T> propertyEnvelope : getListPropertyEnvelopeRepository()) {
            if (propertyEnvelope.getRepositoryPropertyKey().equals(repositoryPropertyKey)) {
                return propertyEnvelope;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public PropertyEnvelope<T> getByPropertyKey(String propertyKey) {
        for (PropertyEnvelope<T> propertyEnvelope : getListPropertyEnvelopeRepository()) {
            if (propertyEnvelope.getPropertyKey().equals(propertyKey)) {
                return propertyEnvelope;
            }
        }
        return null;
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("listPropertyEnvelopeRepository", listPropertyEnvelopeRepository)
                .append("init", init.get());
    }

}
