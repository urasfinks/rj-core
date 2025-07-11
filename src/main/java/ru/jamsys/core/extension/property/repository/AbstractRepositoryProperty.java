package ru.jamsys.core.extension.property.repository;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public abstract class AbstractRepositoryProperty<T> {

    abstract public void init(String ns, boolean sync);

    private final List<PropertyEnvelope<T>> listPropertyEnvelopeRepository = new ArrayList<>();

    private final AtomicBoolean init = new AtomicBoolean(false);

    public void append(String repositoryPropertyKey, String ns) {
    }

    abstract public void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher);

    public void checkNotNull() {
        UtilRisc.forEach(null, getListPropertyEnvelopeRepository(), tPropertyEnvelope -> {
            if (tPropertyEnvelope.isNotNull() && tPropertyEnvelope.getValue() == null) {
                throw new ForwardException("checkNotNull", new HashMapBuilder<String, Object>()
                        .append("problemField", tPropertyEnvelope)
                        .append("repository", this));
            }
        });
    }

    public void checkRegexp() {
        UtilRisc.forEach(null, getListPropertyEnvelopeRepository(), tPropertyEnvelope -> {
            tPropertyEnvelope.checkRegexp();
        });
    }

    public PropertyEnvelope<T> getByFieldNameConstants(String fieldNameConstants) { //@FieldNameConstants -> FileByteProperty.Fields.folder
        for (PropertyEnvelope<T> propertyEnvelope : getListPropertyEnvelopeRepository()) {
            if (propertyEnvelope.getFieldNameConstants().equals(fieldNameConstants)) {
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
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("listPropertyEnvelopeRepository", listPropertyEnvelopeRepository)
                .append("init", init.get());
    }

}
